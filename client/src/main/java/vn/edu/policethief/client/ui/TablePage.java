package vn.edu.policethief.client.ui;

import java.util.List;

/** Display-ready history/ranking page. Views never decode TCP fields. */
public record TablePage(String type, List<List<String>> rows, int page, int pages, int total) {
    public TablePage {
        rows = rows.stream().map(List::copyOf).toList();
    }
}
