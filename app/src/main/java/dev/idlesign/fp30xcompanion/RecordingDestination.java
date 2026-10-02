package dev.idlesign.fp30xcompanion;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

public final class RecordingDestination {
    private static final String KEY_TREE_URI = "recording_tree_uri";
    public enum Kind {
        DOWNLOADS,
        DOCUMENT_TREE
    }

    public final Kind kind;
    public final Uri treeUri;

    private RecordingDestination(Kind kind, Uri treeUri) {
        this.kind = kind;
        this.treeUri = treeUri;
    }

    public static RecordingDestination downloads() {
        return new RecordingDestination(Kind.DOWNLOADS, null);
    }

    public static RecordingDestination tree(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Tree URI is required");
        }
        return new RecordingDestination(Kind.DOCUMENT_TREE, uri);
    }

    public static RecordingDestination load(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("fp30x", Context.MODE_PRIVATE);
        String raw = preferences.getString(KEY_TREE_URI, null);
        return raw == null ? downloads() : tree(Uri.parse(raw));
    }

    public void persist(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences("fp30x", Context.MODE_PRIVATE)
                .edit();
        if (kind == Kind.DOCUMENT_TREE) {
            editor.putString(KEY_TREE_URI, treeUri.toString());
        } else {
            editor.remove(KEY_TREE_URI);
        }
        editor.apply();
    }
}
