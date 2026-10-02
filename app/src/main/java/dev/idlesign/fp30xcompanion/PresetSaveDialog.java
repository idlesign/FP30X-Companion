package dev.idlesign.fp30xcompanion;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.EditText;

public final class PresetSaveDialog {
    public interface Listener {
        void onSave(String name);
    }

    private PresetSaveDialog() {
    }

    public static void show(Context context, String currentName, Listener listener) {
        EditText name = new EditText(context);
        name.setSingleLine(true);
        name.setText(currentName);
        name.setSelectAllOnFocus(true);
        int padding = Math.round(20 * context.getResources().getDisplayMetrics().density);
        name.setPadding(padding, padding / 2, padding, padding / 2);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.preset_save_title)
                .setView(name)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_save, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            UiStyles.dialogButtons(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String value = name.getText().toString().trim();
                if (value.isEmpty()) {
                    name.setError(context.getString(R.string.error_preset_name_empty));
                    return;
                }
                listener.onSave(value);
                dialog.dismiss();
            });
        });
        dialog.show();
    }
}
