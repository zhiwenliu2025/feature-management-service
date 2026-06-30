package com.fms.console.shared.ui.components;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

public final class UnsavedChangesGuard {

  private static final ThreadLocal<Boolean> DIRTY = ThreadLocal.withInitial(() -> false);

  private UnsavedChangesGuard() {}

  public static void markDirty(boolean dirty) {
    DIRTY.set(dirty);
  }

  public static boolean isDirty() {
    return Boolean.TRUE.equals(DIRTY.get());
  }

  public static void confirmIfDirty(Runnable action) {
    if (!isDirty()) {
      action.run();
      return;
    }
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Unsaved changes");
    dialog.setText("You have unsaved draft changes. Continue without saving?");
    dialog.setConfirmText("Discard changes");
    dialog.setCancelText("Stay");
    dialog.setCancelable(true);
    dialog.addConfirmListener(e -> {
      markDirty(false);
      action.run();
    });
    dialog.open();
  }
}
