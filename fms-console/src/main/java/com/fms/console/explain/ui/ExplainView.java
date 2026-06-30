package com.fms.console.explain.ui;

import jakarta.annotation.security.PermitAll;

import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.ExplainDtos.EvaluateContextDto;
import com.fms.console.client.dto.ExplainDtos.ExplainRequestDto;
import com.fms.console.client.dto.ExplainDtos.ExplainResponseDto;
import com.fms.console.client.dto.ExplainDtos.ReplayExplainRequestDto;
import com.fms.console.explain.service.ExplainUiService;
import com.fms.console.explain.ui.components.DecisionTracePanel;
import com.fms.console.shared.ui.AccessControlService;
import com.fms.console.shared.ui.ForbiddenView;
import com.fms.console.shared.ui.GlobalContextBar;
import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.LayoutUiService;
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import com.fms.console.shared.ui.components.FmsNotification;
import com.fms.console.shared.ui.components.PageHeader;
import com.fms.console.shared.ui.components.SectionCard;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import java.util.Map;

@Route(value = "explain", layout = MainLayout.class)
@PermitAll
public class ExplainView extends VerticalLayout implements BeforeEnterObserver {

  private final ExplainUiService explainUiService;
  private final AccessControlService accessControl;
  private final LayoutUiService layoutUi;

  private final VerticalLayout results = new VerticalLayout();
  private boolean showFullUserId;

  public ExplainView(ExplainUiService explainUiService, AccessControlService accessControl, LayoutUiService layoutUi) {
    this.explainUiService = explainUiService;
    this.accessControl = accessControl;
    this.layoutUi = layoutUi;
    setPadding(true);
    setSpacing(true);
    setSizeFull();
    add(new PageHeader("Explain debugger", "Why does a user see or not see a feature?"));
    add(buildForm(), results);
    setFlexGrow(1, results);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!accessControl.canExplain()) {
      event.rerouteTo(ForbiddenView.class);
    }
    layoutUi.setBreadcrumb(new FmsBreadcrumb().current("Explain debugger"));
  }

  private FormLayout buildForm() {
    FormLayout form = new FormLayout();

    TextField flagKey = new TextField("Flag key");
    flagKey.setRequiredIndicatorVisible(true);
    TextField environment = new TextField("Environment");
    environment.setValue(GlobalContextBar.resolveEnvironment());
    TextField userId = new TextField("User ID");
    TextField region = new TextField("Region");
    TextField appVersion = new TextField("App version");
    Checkbox replay = new Checkbox("Historical replay");
    TextField configVersion = new TextField("Config version (replay)");

    Checkbox showPii = new Checkbox("Show full user ID");
    showPii.setEnabled(accessControl.canExplainPii());
    showPii.addValueChangeListener(e -> showFullUserId = Boolean.TRUE.equals(e.getValue()));

    Button run = new Button("Run explain", VaadinIcon.SEARCH.create(), e -> {
      try {
        EvaluateContextDto ctx = new EvaluateContextDto(
            maskUserId(userId.getValue()),
            null,
            region.getValue(),
            appVersion.getValue(),
            Map.of());
        ExplainResponseDto response;
        if (replay.getValue()) {
          Long cv = configVersion.isEmpty() ? null : Long.parseLong(configVersion.getValue());
          response = explainUiService.replay(flagKey.getValue(), new ReplayExplainRequestDto(
              environment.getValue(),
              GlobalContextBar.resolveAppId(),
              cv,
              null,
              ctx,
              true));
        } else {
          response = explainUiService.explain(flagKey.getValue(), new ExplainRequestDto(
              environment.getValue(),
              GlobalContextBar.resolveAppId(),
              ctx,
              true));
        }
        showResult(response);
      } catch (FmsUiException ex) {
        ApiClientExceptionHandler.handle(ex);
      } catch (Exception ex) {
        FmsNotification.error("Invalid input.");
      }
    });
    run.getElement().setAttribute("aria-label", "Run explain query");

    form.add(flagKey, environment, userId, region, appVersion, replay, configVersion, showPii, run);
    return form;
  }

  private String maskUserId(String userId) {
    if (userId == null || userId.isBlank()) {
      return userId;
    }
    if (showFullUserId && accessControl.canExplainPii()) {
      return userId;
    }
    if (userId.length() <= 4) {
      return "usr_***";
    }
    return userId.substring(0, 4) + "***";
  }

  private void showResult(ExplainResponseDto response) {
    results.removeAll();
    VerticalLayout resultContent = new VerticalLayout();
    resultContent.setPadding(false);
    resultContent.setSpacing(true);
    resultContent.add(new DecisionTracePanel(response));
    Button copy = new Button("Copy summary", VaadinIcon.COPY.create(), e -> {
      String summary = "enabled=" + response.enabled() + " value=" + response.value()
          + " reason=" + response.reasonCode();
      getUI().ifPresent(ui -> ui.getPage().executeJs("navigator.clipboard.writeText($0)", summary));
      FmsNotification.success("Summary copied.");
    });
    resultContent.add(copy);
    results.add(new SectionCard("Result", resultContent));
  }
}
