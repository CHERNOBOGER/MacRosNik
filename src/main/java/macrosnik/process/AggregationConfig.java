package macrosnik.process;

public class AggregationConfig {

    /** минимальный интервал между точками траектории */
    public long minMouseMoveIntervalMs = 15;

    /** минимальное расстояние (px), чтобы считать движение значимым */
    public int minMouseMoveDistancePx = 4;

}
