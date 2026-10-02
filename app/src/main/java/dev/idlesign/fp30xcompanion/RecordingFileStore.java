package dev.idlesign.fp30xcompanion;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class RecordingFileStore {
    public static final class OutputHandle implements AutoCloseable {
        public final Uri uri;
        public final OutputStream stream;
        private final Runnable finalizer;
        private boolean closed;

        OutputHandle(Uri uri, OutputStream stream, Runnable finalizer) {
            this.uri = uri;
            this.stream = stream;
            this.finalizer = finalizer;
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            stream.close();
            finalizer.run();
        }
    }

    private final Context context;
    private final ContentResolver resolver;

    public RecordingFileStore(Context context) {
        this.context = context.getApplicationContext();
        resolver = context.getContentResolver();
    }

    public OutputHandle create(
            RecordingDestination destination,
            String displayName,
            String mimeType
    ) throws IOException {
        if (destination.kind == RecordingDestination.Kind.DOCUMENT_TREE) {
            try {
                return createInTree(destination.treeUri, displayName, mimeType);
            } catch (SecurityException | FileNotFoundException error) {
                RecordingDestination.downloads().persist(context);
                throw new IOException(context.getString(R.string.error_recording_directory_lost), error);
            }
        }
        return createInDownloads(displayName, mimeType);
    }

    private OutputHandle createInTree(Uri treeUri, String name, String mime) throws IOException {
        String documentId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
        Uri created = DocumentsContract.createDocument(resolver, parent, mime, name);
        if (created == null) {
            throw new IOException("Could not create " + name);
        }
        OutputStream output = resolver.openOutputStream(created, "w");
        if (output == null) {
            throw new IOException("Could not open " + name);
        }
        return new OutputHandle(created, output, () -> {
        });
    }

    private OutputHandle createInDownloads(String name, String mime) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IOException("Could not create " + name);
            }
            OutputStream output = resolver.openOutputStream(uri, "w");
            if (output == null) {
                resolver.delete(uri, null, null);
                throw new IOException("Could not open " + name);
            }
            return new OutputHandle(uri, output, () -> {
                ContentValues complete = new ContentValues();
                complete.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(uri, complete, null, null);
            });
        }
        File directory = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Could not create Downloads");
        }
        File target = uniqueFile(directory, name);
        return new OutputHandle(Uri.fromFile(target), new FileOutputStream(target), () -> {
        });
    }

    private File uniqueFile(File directory, String name) {
        File result = new File(directory, name);
        if (!result.exists()) {
            return result;
        }
        int dot = name.lastIndexOf('.');
        String stem = dot < 0 ? name : name.substring(0, dot);
        String suffix = dot < 0 ? "" : name.substring(dot);
        int counter = 2;
        while (result.exists()) {
            result = new File(directory, stem + "_" + counter + suffix);
            counter++;
        }
        return result;
    }
}
