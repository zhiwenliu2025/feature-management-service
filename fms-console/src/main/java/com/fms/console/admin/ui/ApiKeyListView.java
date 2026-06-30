package com.fms.console.admin.ui;

import jakarta.annotation.security.PermitAll;

import com.fms.console.admin.service.ApplicationUiService;
import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.ApplicationDtos.ApiKeyCreatedDto;
import com.fms.console.client.dto.ApplicationDtos.ApiKeyDto;
import com.fms.console.client.dto.ApplicationDtos.CreateApiKeyDto;
import com.fms.console.shared.ui.AccessControlService;
import com.fms.console.shared.ui.ForbiddenView;
import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.LayoutUiService;
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import com.fms.console.shared.ui.components.FmsConfirmDialog;
import com.fms.console.shared.ui.components.FmsNotification;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.UUID;

@Route(value = "admin/applications/:appId/keys", layout = MainLayout.class)
@PermitAll
public class ApiKeyListView extends VerticalLayout implements BeforeEnterObserver {

  private final ApplicationUiService applicationUiService;
  private final AccessControlService accessControl;
  private final LayoutUiService layoutUi;
  private String appId;

  public ApiKeyListView(
      ApplicationUiService applicationUiService,
      AccessControlService accessControl,
      LayoutUiService layoutUi) {
    this.applicationUiService = applicationUiService;
    this.accessControl = accessControl;
    this.layoutUi = layoutUi;
    setPadding(true);
    setSpacing(true);
    setSizeFull();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!accessControl.isAdmin()) {
      event.rerouteTo(ForbiddenView.class);
      return;
    }
    appId = event.getRouteParameters().get("appId").orElse("");
    layoutUi.setBreadcrumb(new FmsBreadcrumb()
        .segment("Applications", ApplicationListView.class)
        .current(appId + " / API keys"));
    render();
  }

  private void render() {
    removeAll();
    H2 title = new H2("API keys — " + appId);
    title.addClassName("fms-page-title");
    Button create = new Button("Create API key", e -> openCreate());
    add(title, create);

    Grid<ApiKeyDto> grid = new Grid<>(ApiKeyDto.class, false);
    grid.addColumn(ApiKeyDto::keyPrefix).setHeader("Key prefix");
    grid.addColumn(ApiKeyDto::name).setHeader("Name");
    grid.addColumn(k -> k.scopes() == null ? "" : String.join(", ", k.scopes())).setHeader("Scopes");
    grid.addColumn(ApiKeyDto::createdAt).setHeader("Created");
    grid.addColumn(ApiKeyDto::revokedAt).setHeader("Revoked");
    grid.addComponentColumn(key -> {
      Button revoke = new Button("Revoke", e -> FmsConfirmDialog.confirmDestructive(
          "Revoke API key",
          "Revoke key " + key.keyPrefix() + "? This cannot be undone.",
          () -> revoke(key.id())));
      revoke.setEnabled(key.revokedAt() == null);
      return revoke;
    });
    try {
      grid.setItems(applicationUiService.listApiKeys(appId));
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
    grid.setSizeFull();
    add(grid);
    setFlexGrow(1, grid);
  }

  private void openCreate() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Create API key");
    TextField name = new TextField("Name");
    Button create = new Button("Create", e -> {
      try {
        ApiKeyCreatedDto created = applicationUiService.createApiKey(appId,
            new CreateApiKeyDto(name.getValue(), List.of("flags:read"), null));
        dialog.close();
        showPlaintextOnce(created);
        render();
      } catch (FmsUiException ex) {
        ApiClientExceptionHandler.handle(ex);
      }
    });
    dialog.add(name, create);
    dialog.open();
  }

  private void showPlaintextOnce(ApiKeyCreatedDto created) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("API key created");
    Div warning = new Div();
    warning.setText("Copy this key now. It will not be shown again.");
    warning.addClassName("fms-api-key-warning");
    Paragraph key = new Paragraph(created.apiKey());
    Button copy = new Button("Copy", e -> getUI().ifPresent(ui ->
        ui.getPage().executeJs("navigator.clipboard.writeText($0)", created.apiKey())));
    dialog.add(warning, key, copy);
    dialog.open();
  }

  private void revoke(UUID keyId) {
    try {
      applicationUiService.revokeApiKey(appId, keyId);
      FmsNotification.success("API key revoked.");
      render();
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }
}
