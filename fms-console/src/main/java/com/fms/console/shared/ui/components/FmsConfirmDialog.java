package com.fms.console.shared.ui.components;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog.CancelEvent;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog.ConfirmEvent;

import java.util.function.Consumer;

public final class FmsConfirmDialog {

  private FmsConfirmDialog() {}

  public static void confirm(String header, String message, Runnable onConfirm) {
  confirm(header, message, onConfirm, "Confirm", "Cancel");
  }

  public static void confirm(
      String header, String message, Runnable onConfirm, String confirmText, String cancelText) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader(header);
    dialog.setText(message);
    dialog.setConfirmText(confirmText);
    dialog.setCancelText(cancelText);
    dialog.setCancelable(true);
    dialog.addConfirmListener(event -> onConfirm.run());
    dialog.open();
  }

  public static void confirmDestructive(String header, String message, Runnable onConfirm) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader(header);
    dialog.setText(message);
    dialog.setConfirmText("Confirm");
    dialog.setCancelText("Cancel");
    dialog.setCancelable(true);
    dialog.setConfirmButtonTheme("error primary");
    dialog.addConfirmListener(event -> onConfirm.run());
    dialog.open();
  }
}
