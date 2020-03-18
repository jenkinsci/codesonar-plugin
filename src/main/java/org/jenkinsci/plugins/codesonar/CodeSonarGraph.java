package org.jenkinsci.plugins.codesonar;

import hudson.util.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author andrius
 */
public class CodeSonarGraph {

    protected String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void drawGraph(StaplerRequest req, StaplerResponse rsp, DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb, String title)
            throws IOException {
        try {
            new Graph(-1L, 400, 300) {
                protected JFreeChart createGraph() {
                    return createChart(dsb.build(), title, null);
                }
            }.doPng(req, rsp);
        } catch (RuntimeException ex) {
            throw ex;
        }
    }
    
    protected JFreeChart createChart(CategoryDataset dataset, String title, String yaxis) {
        final JFreeChart chart = ChartFactory.createLineChart(title, // chart                                                                                                                                       // title
                null, // unused
                yaxis, // range axis label
                dataset, // data
                PlotOrientation.VERTICAL, // orientation
                true, // include legend
                true, // tooltips
                false // urls
        );

        final LegendTitle legend = chart.getLegend();

        legend.setPosition(RectangleEdge.BOTTOM);

        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();

        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseStroke(new BasicStroke(2.0f));
        ColorPalette.apply(renderer);
        plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));
        return chart;
    }
}
