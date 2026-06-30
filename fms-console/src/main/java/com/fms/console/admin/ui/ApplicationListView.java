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
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import com.fms.console.shared.ui.components.FmsNotification;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.fms.console.shared.ui.RouteLinks;

@Route(value = "admin/applications", layout = MainLayout.class)
@PermitAll
public class ApplicationListView extends VerticalLayout implements BeforeEnterObserver {

  private final ApplicationUiService applicationUiService;
  private final AccessControlService accessControl;
  private final LayoutUiService layoutUi;
  private final Grid<ApplicationDto> grid = new Grid<>(ApplicationDto.class, false);

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

    H2 title = new H2("Applications");
    title.addClassName("fms-page-title");
    Button create = new Button("Create application", e -> openCreate());
    configureGrid();
    add(title, create, grid);
    setFlexGrow(1, grid);
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
    grid.addColumn(ApplicationDto::slug).setHeader("Slug")
        .setRenderer(new ComponentRenderer<>(a -> RouteLinks.apiKeys(a.slug(), a.slug())));
    grid.addColumn(ApplicationDto::name).setHeader("Name");
    grid.addColumn(ApplicationDto::ownerTeam).setHeader("Owner team");
    grid.addColumn(ApplicationDto::status).setHeader("Status");
    grid.addColumn(ApplicationDto::createdAt).setHeader("Created");
    grid.addComponentColumn(app -> new Button("Edit", e -> openEdit(app)));
    grid.setSizeFull();
  }

  private void load() {
    try {
      PageResponse<ApplicationDto> page = applicationUiService.list(50, null);
      grid.setItems(page.data());
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
