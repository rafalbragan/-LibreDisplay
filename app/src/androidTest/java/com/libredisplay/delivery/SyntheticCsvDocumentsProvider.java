package com.libredisplay.delivery;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/** Platform-only Java: DocumentsUI starts the test APK without the target APK's Kotlin runtime. */
public class SyntheticCsvDocumentsProvider extends DocumentsProvider {
    public static final String CSV = "\uFEFFDane glukozy,Profil DEMO\r\n"
        + "Urządzenie,Numer seryjny,Znacznik czasu w urządzeniu,Rodzaj zapisu,Historyczne stężenia glukozy mg/dL,Stężenie glukozy na podstawie skanowania mg/dL,Ciała ketonowe mmol/L,Notatki\r\n"
        + "Libre,synthetic,01-05-2020 10:00,0,110,,0.2,\r\n"
        + "Libre,synthetic,01-05-2020 10:01,1,,123,0.3,\"Przykład, bez danych osobowych\r\nDruga linia\"\r\n"
        + "Libre,synthetic,01-05-2020 10:02,6,,,0.4,zdarzenie\r\n"
        + "Libre,synthetic,01-05-2020 10:00,0,110,,0.2,\r\n"
        + "Libre,synthetic,01-05-2020 10:00,0,210,,0.2,\r\n"
        + "Libre,synthetic,19-10-2026 10:00,0,145,,0.2,\r\n";
    @Override public boolean onCreate() { return true; }
    @Override public Cursor queryRoots(String[] projection) {
        String[] columns = projection != null ? projection : new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_TITLE, Root.COLUMN_FLAGS, Root.COLUMN_MIME_TYPES };
        MatrixCursor cursor = new MatrixCursor(columns);
        Map<String, Object> values = new HashMap<>();
        values.put(Root.COLUMN_ROOT_ID, "root"); values.put(Root.COLUMN_DOCUMENT_ID, "root");
        values.put(Root.COLUMN_TITLE, "LibreCare DEMO CSV");
        values.put(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_IS_CHILD);
        values.put(Root.COLUMN_MIME_TYPES, "text/csv\ntext/plain");
        append(cursor, columns, values);
        return cursor;
    }
    @Override public Cursor queryDocument(String id, String[] projection) { return documents(projection, new String[] {id}); }
    @Override public Cursor queryChildDocuments(String parent, String[] projection, String sortOrder) {
        return documents(projection, "root".equals(parent) ? new String[] {"valid", "invalid"} : new String[0]);
    }
    @Override public String getDocumentType(String id) { return "root".equals(id) ? Document.MIME_TYPE_DIR : "text/csv"; }
    @Override public boolean isChildDocument(String parent, String id) {
        return "root".equals(parent) && ("valid".equals(id) || "invalid".equals(id));
    }
    @Override public synchronized ParcelFileDescriptor openDocument(String id, String mode, CancellationSignal signal) throws FileNotFoundException {
        if (!"r".equals(mode) || !("valid".equals(id) || "invalid".equals(id))) throw new FileNotFoundException();
        if (signal != null) signal.throwIfCanceled();
        File file = new File(getContext().getCacheDir(), "platform-synthetic-" + id + ".csv");
        try {
            Files.write(file.toPath(), ("valid".equals(id) ? CSV : "unrelated,columns\nnot,libreview").getBytes(StandardCharsets.UTF_8));
        } catch (IOException error) { throw new FileNotFoundException("Synthetic fixture unavailable"); }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
    private Cursor documents(String[] projection, String[] ids) {
        String[] columns = projection != null ? projection : new String[] {
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_FLAGS, Document.COLUMN_SIZE };
        MatrixCursor cursor = new MatrixCursor(columns);
        for (String id : ids) {
            Map<String, Object> values = new HashMap<>();
            values.put(Document.COLUMN_DOCUMENT_ID, id);
            values.put(Document.COLUMN_DISPLAY_NAME, "root".equals(id) ? "LibreCare DEMO CSV" : "valid".equals(id) ? "LibreView-DEMO.csv" : "Niepoprawny-LibreView.csv");
            values.put(Document.COLUMN_MIME_TYPE, getDocumentType(id)); values.put(Document.COLUMN_FLAGS, 0);
            values.put(Document.COLUMN_SIZE, "valid".equals(id) ? CSV.getBytes(StandardCharsets.UTF_8).length : 0);
            append(cursor, columns, values);
        }
        return cursor;
    }
    private void append(MatrixCursor cursor, String[] columns, Map<String, Object> values) {
        Object[] row = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) row[i] = values.get(columns[i]);
        cursor.addRow(row);
    }
}

