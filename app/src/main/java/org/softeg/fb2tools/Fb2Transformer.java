package org.softeg.fb2tools;

import android.content.ContentResolver;
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/*
 * Created by slinkin on 12.05.2015.
 */
public class Fb2Transformer {
    private XmlPullParser parser;
    private String mFilePath;
    private String mEncoding;
    private StringBuilder warnings = new StringBuilder();

    public Fb2Transformer(String path) throws IOException, XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();

        parser = factory.newPullParser();
        mFilePath = path;
        File file = new File(mFilePath);

        parser.setInput(new FileInputStream(file), null);

        parser.nextTag();
        mEncoding = parser.getInputEncoding();
    }

    private HashMap<String, Section> m_Notes = new HashMap<>();

    public void parse() throws Exception {
        parseNotes();
        if (m_Notes.size() != 0) {
            replaceInNotes();
            replaceText(mFilePath);
        }
        else
            addWarning("Сноски не найдены!");
    }

    private void replaceInNotes() {
        for (Section section : m_Notes.values()) {
            if(section.getSectionIds().size()==0)
                continue;
            for(String id: section.getSectionIds()) {
                Section sec = m_Notes.get(id);
                if (sec == null) {
                    addWarning("не найдена сноска с id=" + id);
                    continue;
                }
                String text = section.text.toString();
                section.text = new StringBuilder();
                section.text.append(getString(text, sec));
            }
        }
    }

    public void replaceText(String filePath) throws Exception {


        InputStream inputStream = new FileInputStream(filePath);
        BufferedReader br = null;


        File dir = new File(AppPreferences.getOutputPath());
        Boolean created = true;
        if (!dir.exists())
            created = dir.mkdirs();
        if (!created)
            throw new Exception("Не могу создать папку по пути: " + dir.toString());
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        File file = new File(dir, fileName);
        if (file.exists())
            file.delete();
        created = file.createNewFile();
        if (!created)
            throw new Exception("Не могу создать файл по пути: " + file.toString());

        Writer out = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream, mEncoding));
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), mEncoding));


            String line;
            int lineIndex = 1;

            while ((line = br.readLine()) != null) {
                if (m_NotesIndexes.containsKey(lineIndex)) {
                    for (String sectionName : m_NotesIndexes.get(lineIndex)) {
                        Section section = m_Notes.get(sectionName);
                        if (section == null) {
                            addWarning("не найдена сноска с id=" + sectionName);
                            continue;
                        }
                        line = getString(line, section);
                    }
                    m_NotesIndexes.remove(lineIndex);
                }
                lineIndex++;
                out.write(line);

            }

            for (Section section : m_Notes.values()) {
                if (!section.finded) {
                    addWarning("не найдена ссылка на сноску с id=" + section.getId());
                }
            }

        } finally {
            if (out != null)
                out.close();
            if (br != null)
                br.close();

        }

    }

    private String getString(String line, Section section) {
        Matcher m = section.getPattern().matcher(line);
        if (m.find()) {
            section.finded = true;
            String tag = m.group(2);
            if (tag == null || TextUtils.isEmpty(tag))
                tag = "p";

            line = m.replaceAll(
                    String.format("%s<sup>%s</sup></%s><p><sup>____________</sup></p><p><sup>%s</sup></p><p><sup> </sup></p>\n<%s>",
                            m.group(1),
                            m.group(3),
                            tag, section.text, tag));
            // result = result.replaceAll("<section[^>]*?id=\"" + section.getId() + "\">[\\s\\S]*?</section>", "");
        }
        return line;
    }



    private final ContentResolver contentResolver = App.getInstance().getContentResolver();


    private void parseNotes() throws Exception {
        while (findNotesBody()) {
            parseSections();
        }
    }


    private List<String> mPath = new ArrayList<>();

    private void parseSections() throws Exception {
        Section section = null;
        int startDepth = parser.getDepth();

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.END_TAG) {
                if ("section".equals(parser.getName())) {
                    m_Notes.put(section.getId(), section);
                    continue;
                }
                if ("body".equals(parser.getName())) {// закончилась секция <body name="notes">
                    break;
                }
            }

            String name = parser.getName();
            int depth = parser.getDepth();

            if (parser.getEventType() == XmlPullParser.START_TAG) {
                int index = depth - startDepth - 1;
                while (mPath.size() > index) {
                    mPath.remove(mPath.size() - 1);
                }
                if (mPath.size() == index) {
                    mPath.add(name);
                } else {
                    mPath.set(index, name);
                }
                switch (name) {
                    case "section":
                        section = new Section();
                        section.setId(parser.getAttributeValue(null, "id"));
                        break;
                }

                if (mPath.size() > 0 && "section".equals(mPath.get(0))) {
                    if (mPath.size() > 1 && !"title".equals(mPath.get(1))) {
                        if (section != null) {

                            section.text.append("<").append(name);
                            Boolean isNote = false;
                            String href = "";
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                String prefix = parser.getAttributePrefix(i);

                                section.text.append(" ");
                                if (!TextUtils.isEmpty(prefix)) {// для элементов типа <image l:href="..."/>
                                    section.text.append(prefix).append(":");
                                }

                                section.text.append(parser.getAttributeName(i))
                                        .append("=\"")
                                        .append(parser.getAttributeValue(i))
                                        .append("\"");

                                String attrName = parser.getAttributeName(i);
                                String attrValue = parser.getAttributeValue(i);
                                if (!isNote && "type".equals(attrName) &&
                                        "note".equals(attrValue)) {
                                    isNote = true;
                                }
                                if (attrName!=null&&attrName.matches("(\\w:)?href")) {
                                    if (!isNote)
                                        isNote = attrValue.startsWith("#");
                                    href = attrValue;
                                }

                            }

                            if(isNote)
                                section.getSectionIds().add(href.substring(1));

                            if (parser.isEmptyElementTag()) {// для элементов типа <empty-line/> или <image href="..."/>
                                section.text.append("/>");
                                parser.next();
                                continue;
                            }
                            section.text.append(">");
                        }
                    }
                }
            }
            if (parser.getEventType() == XmlPullParser.END_TAG) {
                if (mPath.size() > 0 && "section".equals(mPath.get(0))) {
                    if (mPath.size() > 1 && !"title".equals(mPath.get(1))) {
                        if (section != null)
                            section.text.append("</").append(name).append(">");
                    }
                }
            }
            if (parser.getEventType() == XmlPullParser.TEXT) {
                String text = parser.getText();
                if (text != null && !TextUtils.isEmpty(text.trim())) {
                    if (section != null)
                        if (mPath.size() > 0 && "section".equals(mPath.get(0))) {
                            if (mPath.size() > 1 && "title".equals(mPath.get(1))) {
                                section.title.append(AppHtml.escapeHtml(text));
                            }
                            if (mPath.size() > 1 && !"title".equals(mPath.get(1))) {
                                section.text
                                        .append("<sup>")
                                        .append(AppHtml.escapeHtml(text))
                                        .append("</sup>");
                            }
                        }
                }

            }
        }


    }


    private Map<Integer, List<String>> m_NotesIndexes = new HashMap<>();

    private Boolean findNotesBody() throws IOException, XmlPullParserException {
        int type = parser.getEventType();
        while (type != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {


                String name = parser.getName();

                if ("a".equals(name)) {
                    Boolean isNote = false;
                    String href = "";
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        String attrName = parser.getAttributeName(i);
                        String attrValue = parser.getAttributeValue(i);
                        if (!isNote && "type".equals(attrName) &&
                                "note".equals(attrValue)) {
                            isNote = true;
                        }
                        if (attrName!=null&&attrName.matches("(\\w:)?href")) {
                            if (!isNote)
                                isNote = attrValue.startsWith("#");
                            href = attrValue;
                        }
                    }
                    if (isNote) {
                        int lineNumber = parser.getLineNumber();
                        if (!m_NotesIndexes.containsKey(lineNumber))
                            m_NotesIndexes.put(lineNumber, new ArrayList<String>());
                        if (TextUtils.isEmpty(href)) {
                            m_NotesIndexes.get(lineNumber).add("#empty");
                        } else
                            m_NotesIndexes.get(lineNumber).add(href.substring(1));
                    }
                }

                // Starts by looking for the entry tag
                if (parser.getDepth() == 2 && name.equals("body")) {
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        String attrValue = parser.getAttributeValue(i);
                        if ("name".equals(parser.getAttributeName(i)) &&
                                ("notes".equals(attrValue) || "comments".equals(attrValue))) {
                            return true;
                        }
                    }
                }
            }
            try {
                type = parser.next();
            } catch (XmlPullParserException e) {
            }
        }
        return false;
    }


    private void addWarning(String warning) {
        warnings.append(warning).append("<br/ >\n");
    }

    public StringBuilder getWarnings() {
        return warnings;
    }
}
