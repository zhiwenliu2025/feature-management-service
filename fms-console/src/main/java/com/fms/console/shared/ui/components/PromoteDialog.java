package com.fms.console.shared.ui.components;

import com.fms.console.admin.service.EnvironmentUiService;
import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.EnvironmentDtos.PromoteDto;
import com.fms.console.shared.ui.GlobalContextBar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;
import java.util.Set;

public class PromoteDialog extends Dialog {

  public PromoteDialog(
      EnvironmentUiService environmentUiService,
      String appId,
      List<String> preselectedFlags,
      Runnable onSuccess) {
    setHeaderTitle("Promote to environment");

    ComboBox<String> source = new ComboBox<>("Source environment");
    source.setItems("dev", "staging", "prod");
    source.setValue("staging");

    ComboBox<String> target = new ComboBox<>("Target environment");
    target.setItems("prod");
    target.setValue("prod");
    target.setReadOnly(true);

    MultiSelectComboBox<String> flags = new MultiSelectComboBox<>("Flags");
    flags.setItems(preselectedFlags);
    flags.setValue(Set.copyOf(preselectedFlags));

    TextField release = new TextField("Release ID");
    TextField comment = new TextField("Comment");
    comment.setRequired(true);
    comment.setWidthFull();

    Button promote = new Button("Promote to prod", e -> {
      if (comment.isEmpty()) {
        FmsNotification.error("Comment is required.");
        return;
      }
      try {
        environmentUiService.promote(target.getValue(), new PromoteDto(
            source.getValue(),
            flags.getSelectedItems().stream().toList(),
            appId,
            release.getValue(),
            comment.getValue()));
        FmsNotification.success("Promotion started.");
        close();
        if (onSuccess != null) {
          onSuccess.run();
        }
      } catch (FmsUiException ex) {
        ApiClientExceptionHandler.handle(ex);
      }
    });
    promote.getElement().setAttribute("aria-label", "Promote flags");

    add(source, target, flags, release, comment, promote);
  }
}
