package org.mabb.fontverter.opentype;

import org.mabb.fontverter.io.FontDataOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameTable extends OpenTypeTable {
    static final int NAME_TABLE_HEADER_SIZE = 6;
    private static Logger log = LoggerFactory.getLogger(NameTable.class);
    private static final OtfNameConstants.Language defaultLanguage = OtfNameConstants.Language.UNITED_STATES;

    private List<NameRecord> nameRecords = new ArrayList<NameRecord>();
    private int formatSelector = 0;

    public static NameTable createDefaultTable() {
        NameTable table = new NameTable();

        table.setCopyright("Default Copyright");
        table.setFontFamily("DefaultFamily");
        table.setFontSubFamily("DefaultSubFamily");
        table.setVersion("Version 1.1");
        table.setUniqueId(UUID.randomUUID().toString().replace("-", ""));
        table.setFontFullName("DefaultFontFullName");
        table.setPostScriptName("DefaultPostScriptName");

        return table;
    }

    /* big old kludge to handle conversion of tables types that arn't deserializable/parsable yet remove asap*/
    protected boolean isParsingImplemented() {
        return false;
    }

    public String getTableTypeName() {
        return "name";
    }

    public String getName(OtfNameConstants.RecordType type) {
        for (NameRecord recordOn : nameRecords)
            if (recordOn.nameID == type.getValue())
                return recordOn.getRawString();

        return null;
    }

    private void addName(String name, OtfNameConstants.RecordType type, OtfNameConstants.Language language) {
        deleteExisting(type, language);
        NameRecord windowsRecord = NameRecord.createWindowsRecord(name, type, language);
        nameRecords.add(windowsRecord);

        NameRecord macRecord = NameRecord.createMacRecord(name, type, language);
        nameRecords.add(macRecord);
    }

    private void deleteExisting(OtfNameConstants.RecordType type, OtfNameConstants.Language language) {
        List<NameRecord> deleteList = new LinkedList<NameRecord>();
        for (NameRecord recordOn : nameRecords)
            if (recordOn.nameID == type.getValue())
                deleteList.add(recordOn);

        for (NameRecord recordOn : deleteList)
            nameRecords.remove(recordOn);
    }

    protected byte[] generateUnpaddedData() throws IOException {
        FontDataOutputStream writer = new FontDataOutputStream(FontDataOutputStream.OPEN_TYPE_CHARSET);
        writer.writeUnsignedShort(formatSelector);
        writer.writeUnsignedShort(nameRecords.size());
        writer.writeUnsignedShort(getOffsetToStringStorage());

        calculateOffsets();

        Collections.sort(nameRecords, new Comparator<NameRecord>() {
            @Override
            public int compare(NameRecord o1, NameRecord o2) {
                if (o1.platformID != o2.platformID)
                    return o1.platformID < o2.platformID ? -1 : 1;
                return o1.nameID < o2.nameID ? -1 : o1.nameID == o2.nameID ? 0 : 1;

            }
        });

        for (NameRecord record : nameRecords)
            writer.write(record.getRecordData());

        for (NameRecord record : nameRecords)
            writer.write(record.getStringData());

        return writer.toByteArray();
    }

    private void calculateOffsets() throws IOException {
        int offset = 0;
        for (NameRecord recordOn : nameRecords) {
            recordOn.setOffset(offset);
            offset += recordOn.getLength();
        }
    }

    private int getOffsetToStringStorage() {
        return NAME_TABLE_HEADER_SIZE + (NameRecord.NAME_RECORD_SIZE * nameRecords.size());
    }

    private String formatVersion(String version) {
        String versionNumber = "";

        Matcher versionRegex = Pattern.compile("[1-9][0-9]*[.][0-9]*").matcher(version);
        if (versionRegex.find())
            versionNumber = versionRegex.group(0);

        if (versionNumber.isEmpty()) {
            Matcher noPeriodVersionRegex = Pattern.compile("[0-9]*").matcher(version);
            versionNumber = noPeriodVersionRegex.group(0) + ".0";
        }

        if (versionNumber.isEmpty())
            versionNumber = "1.1";

        return "Version " + versionNumber;
    }

    public void setFontFamily(String family) {
        addName(family, OtfNameConstants.RecordType.FONT_FAMILY, defaultLanguage);
    }

    public void setCopyright(String name) {
        addName(name, OtfNameConstants.RecordType.COPYRIGHT, defaultLanguage);
    }

    public void setFontSubFamily(String name) {
        addName(name, OtfNameConstants.RecordType.FONT_SUB_FAMILY, defaultLanguage);
    }

    public void setFontFullName(String name) {
        addName(name, OtfNameConstants.RecordType.FULL_FONT_NAME, defaultLanguage);
    }

    public void setUniqueId(String name) {
        addName(name, OtfNameConstants.RecordType.UNIQUE_FONT_ID, defaultLanguage);
    }

    public void setPostScriptName(String name) {
        addName(name, OtfNameConstants.RecordType.POSTSCRIPT_NAME, defaultLanguage);
    }

    public void setVersion(String name) {
        addName(formatVersion(name), OtfNameConstants.RecordType.VERSION_STRING, defaultLanguage);
    }
}