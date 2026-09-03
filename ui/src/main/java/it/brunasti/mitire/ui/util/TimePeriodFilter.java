package it.brunasti.mitire.ui.util;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * A "Period" dropdown (all time / last week / a chosen week / last month / a chosen
 * month / a single chosen day / a custom from-to range), paired with the date picker(s)
 * each mode needs. {@link #getRange()} resolves the current selection to a concrete
 * [from, to] date range for {@code TimeEntryService.search()}.
 */
public class TimePeriodFilter extends HorizontalLayout {

    public enum Period {
        ALL_TIME("All time"),
        LAST_WEEK("Last week"),
        SELECTED_WEEK("Select week"),
        LAST_MONTH("Last month"),
        SELECTED_MONTH("Select month"),
        SELECTED_DAY("Select day"),
        CUSTOM_RANGE("From - To");

        private final String label;

        Period(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public record DateRange(LocalDate from, LocalDate to) {
    }

    private final Select<Period> period = new Select<>();
    private final DatePicker anchorDate = new DatePicker("Date");
    private final DatePicker fromDate = new DatePicker("From");
    private final DatePicker toDate = new DatePicker("To");

    public TimePeriodFilter() {
        period.setLabel("Period");
        period.setItems(Period.values());
        period.setValue(Period.ALL_TIME);

        anchorDate.setValue(LocalDate.now());
        anchorDate.setVisible(false);
        anchorDate.setWidth("160px");

        fromDate.setVisible(false);
        fromDate.setWidth("160px");
        toDate.setVisible(false);
        toDate.setWidth("160px");

        period.addValueChangeListener(e -> {
            Period selected = e.getValue();
            anchorDate.setVisible(selected == Period.SELECTED_WEEK || selected == Period.SELECTED_MONTH
                    || selected == Period.SELECTED_DAY);
            boolean customRange = selected == Period.CUSTOM_RANGE;
            fromDate.setVisible(customRange);
            toDate.setVisible(customRange);
        });

        setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        setSpacing(true);
        setPadding(false);
        add(period, anchorDate, fromDate, toDate);
    }

    public void addFilterChangeListener(Runnable listener) {
        period.addValueChangeListener(e -> listener.run());
        anchorDate.addValueChangeListener(e -> listener.run());
        fromDate.addValueChangeListener(e -> listener.run());
        toDate.addValueChangeListener(e -> listener.run());
    }

    public DateRange getRange() {
        LocalDate anchor = anchorDate.getValue() != null ? anchorDate.getValue() : LocalDate.now();
        return switch (period.getValue()) {
            case ALL_TIME -> new DateRange(null, null);
            case LAST_WEEK -> weekRange(LocalDate.now().minusWeeks(1));
            case SELECTED_WEEK -> weekRange(anchor);
            case LAST_MONTH -> monthRange(LocalDate.now().minusMonths(1));
            case SELECTED_MONTH -> monthRange(anchor);
            case SELECTED_DAY -> new DateRange(anchor, anchor);
            case CUSTOM_RANGE -> new DateRange(fromDate.getValue(), toDate.getValue());
        };
    }

    private static DateRange weekRange(LocalDate dayInWeek) {
        LocalDate start = dayInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new DateRange(start, start.plusDays(6));
    }

    private static DateRange monthRange(LocalDate dayInMonth) {
        LocalDate start = dayInMonth.withDayOfMonth(1);
        return new DateRange(start, dayInMonth.with(TemporalAdjusters.lastDayOfMonth()));
    }
}
