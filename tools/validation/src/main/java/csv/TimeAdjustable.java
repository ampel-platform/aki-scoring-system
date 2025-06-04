package csv;

public interface TimeAdjustable<T> {
    T offsetTimesBySeconds(long offsetSeconds);
}
