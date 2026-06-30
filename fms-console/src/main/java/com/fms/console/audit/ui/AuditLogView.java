package com.fms.console.audit.ui;

import jakarta.annotation.security.PermitAll;

import com.fms.common.api.PageResponse;
import com.fms.console.audit.service.AuditUiService;
import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.AuditDtos.AuditEventDto;
import com.fms.console.flag.ui.FlagDetailView;
import com.fms.console.shared.ui.AccessControlService;
import com.fms.console.shared.ui.ForbiddenView;
import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.LayoutUiService;
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import tools.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.fms.console.shared.ui.RouteLinks;

import java.time.ZoneId;

@Route(value = "audit", layout = MainLayout.class)
@PermitAll
public class AuditLogView extends VerticalLayout implements BeforeEnterObserver {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final AuditUiService auditUiService;
  private final AccessControlService accessControl;
  private final LayoutUiService layoutUi;

  private final Grid<AuditEventDto> grid = new Grid<>(AuditEventDto.class, false);
  private final DateTimePicker from = new DateTimePicker("From");
  private final DateTimePicker to = new DateTimePicker("To");
  private final ComboBox<String> action = new ComboBox<>("Action");
  private final TextField actor = new TextField("Actor");
  private final TextField resource = new TextField("Resource");
  private final TextField environment = new TextField("Environment");

  public AuditLogView(AuditUiService auditUiService, AccessControlService accessControl, LayoutUiService layoutUi) {
    this.auditUiService = auditUiService;
    this.accessControl = accessControl;
    this.layoutUi = layoutUi;

    setPadding(true);
    setSpacing(true);
    setSizeFull();

    action.setItems("publish", "rollback", "kill_switch_on", "kill_switch_off", "flag_create", "flag_update");
    action.setClearButtonVisible(true);

    H2 title = new H2("Audit log");
    title.addClassName("fms-page-title");
    Button search = new Button("Search", e -> load(null));
    HorizontalLayout filters = new HorizontalLayout(from, to, action, actor, resource, environment, search);
    filters.setWidthFull();
    configureGrid();
    add(title, filters, grid);
    setFlexGrow(1, grid);
    load(null);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!accessControl.canReadAudit()) {
      event.rerouteTo(ForbiddenView.class);
    }
    layoutUi.setBreadcrumb(new FmsBreadcrumb().current("Audit log"));
  }

  private void configureGrid() {
    grid.addColumn(AuditEventDto::createdAt).setHeader("Time").setFlexGrow(1);
    grid.addColumn(AuditEventDto::actor).setHeader("Actor");
    grid.addColumn(AuditEventDto::action).setHeader("Action");
    grid.addColumn(event -> event.resourceId()).setHeader("Resource")
        .setRenderer(new ComponentRenderer<>(event -> {
          if ("feature_flag".equals(event.resourceType())) {
            return RouteLinks.flag(event.resourceId(), event.resourceId());
          }
          return new com.vaadin.flow.component.html.Span(event.resourceId());
        }));
    grid.addColumn(AuditEventDto::environment).setHeader("Environment");
    grid.addComponentColumn(event -> {
      try {
        String json = MAPPER.writeValueAsString(event.diff());
        Details details = new Details("Diff", new Pre(json));
        return details;
      } catch (Exception e) {
        return new com.vaadin.flow.component.html.Span("—");
      }
    }).setHeader("Detail").setFlexGrow(2);
    grid.setSizeFull();
  }

  private void load(String cursor) {
    try {
      PageResponse<AuditEventDto> page = auditUiService.query(
          null,
          resource.getValue(),
          actor.getValue(),
          action.getValue(),
          environment.getValue(),
          from.getValue() == null ? null : from.getValue().atZone(ZoneId.systemDefault()).toInstant(),
          to.getValue() == null ? null : to.getValue().atZone(ZoneId.systemDefault()).toInstant(),
          50,
          cursor);
      grid.setItems(page.data());
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }
}
