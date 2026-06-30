package com.fms.console.release.ui;

import jakarta.annotation.security.PermitAll;

import com.fms.common.api.PageResponse;
import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.ReleaseDtos.CreateReleaseDto;
import com.fms.console.client.dto.ReleaseDtos.ReleaseSummaryDto;
import com.fms.console.release.service.ReleaseUiService;
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

import java.util.Map;

@Route(value = "releases", layout = MainLayout.class)
@PermitAll
public class ReleaseListView extends VerticalLayout implements BeforeEnterObserver {

  private final ReleaseUiService releaseUiService;
  private final AccessControlService accessControl;
  private final LayoutUiService layoutUi;
  private final Grid<ReleaseSummaryDto> grid = new Grid<>(ReleaseSummaryDto.class, false);

  public ReleaseListView(ReleaseUiService releaseUiService, AccessControlService accessControl, LayoutUiService layoutUi) {
    this.releaseUiService = releaseUiService;
    this.accessControl = accessControl;
    this.layoutUi = layoutUi;
    setPadding(true);
    setSpacing(true);
    setSizeFull();

    H2 title = new H2("Releases");
    title.addClassName("fms-page-title");
    Button create = new Button("Create release", e -> openCreate());
    create.setEnabled(accessControl.canWriteFlags());
    configureGrid();
    add(title, create, grid);
    setFlexGrow(1, grid);
    load();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!accessControl.canReadFlags()) {
      event.rerouteTo(ForbiddenView.class);
    }
    layoutUi.setBreadcrumb(new FmsBreadcrumb().current("Releases"));
  }

  private void configureGrid() {
    grid.addColumn(ReleaseSummaryDto::releaseId).setHeader("Release ID")
        .setRenderer(new ComponentRenderer<>(r -> RouteLinks.release(r.releaseId(), r.releaseId())));
    grid.addColumn(ReleaseSummaryDto::version).setHeader("Version");
    grid.addColumn(ReleaseSummaryDto::title).setHeader("Title");
    grid.addColumn(ReleaseSummaryDto::createdAt).setHeader("Created");
    grid.addColumn(ReleaseSummaryDto::createdBy).setHeader("Created by");
    grid.setSizeFull();
  }

  private void load() {
    try {
      PageResponse<ReleaseSummaryDto> page = releaseUiService.list(50);
      grid.setItems(page.data());
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }

  private void openCreate() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Create release");
    TextField releaseId = new TextField("Release ID");
    TextField version = new TextField("Version");
    TextField title = new TextField("Title");
    Button save = new Button("Create", e -> {
      try {
        releaseUiService.create(new CreateReleaseDto(
            releaseId.getValue(), version.getValue(), title.getValue(), "", Map.of()));
        dialog.close();
        FmsNotification.success("Release created.");
        load();
      } catch (FmsUiException ex) {
        ApiClientExceptionHandler.handle(ex);
      }
    });
    dialog.add(releaseId, version, title, save);
    dialog.open();
  }
}
