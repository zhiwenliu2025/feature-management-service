package com.fms.console.release.ui;

import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.ReleaseDtos.LinkedFlagDto;
import com.fms.console.client.dto.ReleaseDtos.ReleaseDetailDto;
import com.fms.console.release.service.ReleaseUiService;
import com.fms.console.shared.ui.AccessControlService;
import com.fms.console.shared.ui.ForbiddenView;
import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.LayoutUiService;
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "releases/:releaseId", layout = MainLayout.class)
@PermitAll
public class ReleaseDetailView extends VerticalLayout implements BeforeEnterObserver {

  private final ReleaseUiService releaseUiService;
  private final AccessControlService accessControl;
  private final LayoutUiService layoutUi;

  public ReleaseDetailView(ReleaseUiService releaseUiService, AccessControlService accessControl, LayoutUiService layoutUi) {
    this.releaseUiService = releaseUiService;
    this.accessControl = accessControl;
    this.layoutUi = layoutUi;
    setPadding(true);
    setSpacing(true);
    setSizeFull();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!accessControl.canReadFlags()) {
      event.rerouteTo(ForbiddenView.class);
      return;
    }
    String releaseId = event.getRouteParameters().get("releaseId").orElse("");
    layoutUi.setBreadcrumb(new FmsBreadcrumb()
        .segment("Releases", ReleaseListView.class)
        .current(releaseId));
    load(releaseId);
  }

  private void load(String releaseId) {
    removeAll();
    try {
      ReleaseDetailDto release = releaseUiService.get(releaseId);
      H2 title = new H2(release.releaseId());
      title.addClassName("fms-page-title");
      add(title,
          new Paragraph(release.title() + " · v" + release.version()),
          new Paragraph(release.description() == null ? "" : release.description()));

      if (release.metadata() != null && !release.metadata().isEmpty()) {
        add(new Paragraph("CI metadata: " + release.metadata()));
      }

      Grid<LinkedFlagDto> grid = new Grid<>(LinkedFlagDto.class, false);
      grid.addColumn(LinkedFlagDto::flagKey).setHeader("Flag");
      grid.addColumn(LinkedFlagDto::appId).setHeader("Application");
      grid.addColumn(LinkedFlagDto::environment).setHeader("Environment");
      grid.addColumn(LinkedFlagDto::configVersion).setHeader("Config version");
      grid.setItems(release.flags() == null ? java.util.List.of() : release.flags());
      grid.setSizeFull();
      add(grid);
      setFlexGrow(1, grid);
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }
}
