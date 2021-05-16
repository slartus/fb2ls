package org.softeg.fb2tools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/*
 * Created by slinkin on 12.05.2015.
 */
public class Section {
    private String id;
    public Boolean finded = false;
    public StringBuilder title = new StringBuilder();
    public StringBuilder text = new StringBuilder();

    private List<String> sectionIds = new ArrayList<>();// ссылки на другие сноски
    private Pattern mPattern;

    public Pattern getPattern() {
        if (mPattern == null) {
            mPattern = Pattern.compile(String.format("((?:<\\s*(\\w+)[^>]*>)?[^<>]*)?<a[^>]*?href=\"#%s\"[^>]*>(.*?)</a>",
                    id), Pattern.CASE_INSENSITIVE);
        }
        return mPattern;
    }

    public void setId(String id) {
        this.id = id;
        mPattern = null;
    }

    public String getId() {
        return id;
    }

    public List<String> getSectionIds() {
        return sectionIds;
    }
}
