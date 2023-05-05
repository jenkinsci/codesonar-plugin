package org.jenkinsci.plugins.codesonar.models;

import java.util.ArrayList;
import java.util.List;

import org.javatuples.Pair;

/**
 * @author aseno
 *
 */
public class SearchConfigData {
    public enum SortingOrder {ASCENDING, DESCENDING}
    
    public boolean count;
    public int offset;
    public int limit;
    public List<Pair<String, String>> orderBy;
    public List<String> columns;
    
    public SearchConfigData() {
        this.count = false;
        this.offset = 0;
        this.limit = 1;
        this.orderBy = new ArrayList<>();
        this.columns = new ArrayList<>();
    }

    public boolean isCount() {
        return count;
    }

    public void setCount(boolean count) {
        this.count = count;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public List<Pair<String, String>> getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(List<Pair<String, String>> orderBy) {
        this.orderBy = orderBy;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    @Override
    public String toString() {
        return "AnalysisProceduresConfigData [count=" + count + ", offset=" + offset + ", limit=" + limit + ", orderBy="
                + orderBy + ", columns=" + columns + "]";
    }
    
    public void addOrderByCondition(String columnName, SortingOrder order) {
        orderBy.add(new Pair<String, String>(columnName, order.name()));
    }
}
