package com.fms.console.admin.ui;

import jakarta.annotation.security.PermitAll;

import com.fms.common.api.PageResponse;
import com.fms.console.admin.service.ApplicationUiService;
import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.ApplicationDtos.ApplicationDto;
import com.fms.console.client.dto.ApplicationDtos.CreateApplicationDto;
import com.fms.console.client.dto.ApplicationDtos.UpdateApplicationDto;
import com.fms.console.shared.ui.AccessControlService;
import com.fms.console.shared.ui.ForbiddenView;
import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.LayoutUiService;
import com.fms.console.shared.ui.components.EmptyState;
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import com.fms.console.shared.ui.components.FmsNotification;
import com.fms.console.shared.ui.components.PageHeader;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.fms.console.shared.ui.RouteLinks;
import com.fms.console.shared.ui.UiFormat;

import java.util.List;

@Route(value = "admin/applications", layout = MainLayout.class)
@PermitAll
public class ApplicationListView extends VerticalLayout implements BeforeEnterObserver {

  private final ApplicationUiService applicationUiService;
  private final AccessControlService accessControl;
  private final LayoutUiService layoutUi;
  private final Grid<ApplicationDto> grid = new Grid<>(ApplicationDto.class, false);
  private final VerticalLayout gridContainer = new VerticalLayout();

  public ApplicationListView(
      ApplicationUiService applicationUiService,
      AccessControlService accessControl,
      LayoutUiService layoutUi) {
    this.applicationUiService = applicationUiService;
    this.accessControl = accessControl;
    this.layoutUi = layoutUi;
    setPadding(true);
    setSpacing(true);
    setSizeFull();

    Button create = new Button("Create application", VaadinIcon.PLUS.create(), e -> openCreate());
    configureGrid();
    gridContainer.setPadding(false);
    gridContainer.setSizeFull();
    gridContainer.add(grid);
    add(new PageHeader("Applications", create), gridContainer);
    setFlexGrow(1, gridContainer);
    load();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!accessControl.isAdmin()) {
      event.rerouteTo(ForbiddenView.class);
    }
    layoutUi.setBreadcrumb(new FmsBreadcrumb().current("Applications"));
  }

  private void configureGrid() {
    grid.addClassName("fms-grid-compact");
    grid.addColumn(ApplicationDto::slug).setHeader("Slug")
        .setRenderer(new ComponentRenderer<>(a -> {
          var link = RouteLinks.apiKeys(a.slug(), a.slug());
          link.addClassName("fms-monospace");
          return link;
        }));
    grid.addColumn(ApplicationDto::name).setHeader("Name");
    grid.addColumn(ApplicationDto::ownerTeam).setHeader("Owner team");
    grid.addColumn(ApplicationDto::status).setHeader("Status");
    grid.addColumn(a -> UiFormat.formatInstant(a.createdAt())).setHeader("Created");
    grid.addComponentColumn(app -> new Button("Edit", VaadinIcon.EDIT.create(), e -> openEdit(app)));
    grid.setSizeFull();
  }

  private void updateEmptyState(List<ApplicationDto> items) {
    gridContainer.removeAll();
    if (items.isEmpty()) {
      gridContainer.add(new EmptyState(
          "No applications",
          "Onboard an application to manage feature flags.",
          "Create application",
          this::openCreate));
    } else {
      gridContainer.add(grid);
    }
  }

  private void load() {
    try {
      PageResponse<ApplicationDto> page = applicationUiService.list(50, null);
      grid.setItems(page.data());
      updateEmptyState(page.data());
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }

  private void openCreate() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Create application");
    TextField slug = new TextField("Slug");
    TextField name = new TextField("Name");
    TextField team = new TextField("Owner team");
    Button save = new Button("Create", e -> {
      try {
        applicationUiService.create(new CreateApplicationDto(
            slug.getValue(), name.getValue(), "", team.getValue()));
        dialog.close();
        FmsNotification.success("Application created.");
        load();
      } catch (FmsUiException ex) {
        ApiClientExceptionHandler.handle(ex);
      }
    });
    dialog.add(slug, name, team, save);
    dialog.open();
  }

  private void openEdit(ApplicationDto app) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Edit " + app.slug());
    TextField name = new TextField("Name");
    name.setValue(app.name());
    TextField team = new TextField("Owner team");
    team.setValue(app.ownerTeam() == null ? "" : app.ownerTeam());
    Button save = new Button("Save", e -> {
      try {
        applicationUiService.update(app.slug(), new UpdateApplicationDto(
            name.getValue(), app.description(), team.getValue()));
        dialog.close();
        FmsNotification.success("Application updated.");
        load();
      } catch (FmsUiException ex) {
        ApiClientExceptionHandler.handle(ex);
      }
    });
    dialog.add(name, team, save);
    dialog.open();
  }
}
