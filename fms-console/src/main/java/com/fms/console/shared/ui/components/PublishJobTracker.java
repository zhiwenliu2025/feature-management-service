package com.fms.console.shared.ui.components;

import com.fms.console.client.dto.FlagDtos.FlagDetailDto;
import com.fms.console.flag.service.FlagUiService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PublishJobTracker extends HorizontalLayout {

  private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

  private final FlagUiService flagUiService;
  private ScheduledFuture<?> pollTask;

  public PublishJobTracker(FlagUiService flagUiService) {
    this.flagUiService = flagUiService;
    addClassName("fms-publish-tracker");
    setAlignItems(Alignment.CENTER);
    setWidthFull();
  }

  public void track(String appId, String flagKey, String environment, Runnable onComplete) {
    removeAll();
    ProgressBar bar = new ProgressBar();
    bar.setIndeterminate(true);
    Span label = new Span("Publishing to " + environment + "…");
    add(label, bar);

    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    pollTask = EXECUTOR.scheduleAtFixedRate(() -> ui.access(() -> {
      try {
        FlagDetailDto flag = flagUiService.getFlag(appId, flagKey);
        boolean dirty = flag.environmentStates().stream()
            .filter(s -> environment.equals(s.environment()))
            .anyMatch(s -> s.draftDirty());
        if (!dirty) {
          removeAll();
          add(new StatusBadge(StatusBadge.Variant.JOB_COMPLETED));
          FmsNotification.success("Publish completed for " + flagKey);
          if (onComplete != null) {
            onComplete.run();
          }
          stop();
        }
      } catch (Exception ignored) {
        // keep polling
      }
    }), 2, 2, TimeUnit.SECONDS);
  }

  public void stop() {
    if (pollTask != null) {
      pollTask.cancel(true);
      pollTask = null;
    }
  }

  @Override
  public void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
    stop();
    super.onDetach(detachEvent);
  }
}
