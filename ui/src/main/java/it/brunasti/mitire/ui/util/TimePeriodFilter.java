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
 * month) paired with a date picker that only appears for the two "chosen" modes, used
 * to pick any day within the desired week or month. {@link #getRange()} resolves the
 * current selection to a concrete [from, to] date range for {@code TimeEntryService.search()}.
 */
public class TimePeriodFilter extends HorizontalLayout {

    public enum Period {
        ALL_TIME("All time"),
        LAST_WEEK("Last week"),
        SELECTED_WEEK("Select week"),
        LAST_MONTH("Last month"),
        SELECTED_MONTH("Select month");

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
    private final DatePicker anchorDate = new DatePicker();

    public TimePeriodFilter() {
        period.setLabel("Period");
        period.setItems(Period.values());
        period.setValue(Period.ALL_TIME);

        anchorDate.setLabel("Any day in it");
        anchorDate.setValue(LocalDate.now());
        anchorDate.setVisible(false);
        anchorDate.setWidth("160px");

        period.addValueChangeListener(e ->
                anchorDate.setVisible(e.getValue() == Period.SELECTED_WEEK || e.getValue() == Period.SELECTED_MONTH));

        setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        setSpacing(true);
        setPadding(false);
        add(period, anchorDate);
    }

    public void addFilterChangeListener(Runnable listener) {
        period.addValueChangeListener(e -> listener.run());
        anchorDate.addValueChangeListener(e -> listener.run());
    }

    public DateRange getRange() {
        LocalDate anchor = anchorDate.getValue() != null ? anchorDate.getValue() : LocalDate.now();
        return switch (period.getValue()) {
            case ALL_TIME -> new DateRange(null, null);
            case LAST_WEEK -> weekRange(LocalDate.now().minusWeeks(1));
            case SELECTED_WEEK -> weekRange(anchor);
            case LAST_MONTH -> monthRange(LocalDate.now().minusMonths(1));
            case SELECTED_MONTH -> monthRange(anchor);
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
