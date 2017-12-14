/*
 * Copyright 2017, OpenCensus Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opencensus.examples.stats;

import io.opencensus.common.Scope;
import io.opencensus.exporter.stats.signalfx.SignalFxStatsExporter;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.Stats;
import io.opencensus.stats.View;
import io.opencensus.stats.View.AggregationWindow;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import io.opencensus.tags.Tags;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Example application that reports its own heap usage as stats to SignalFx.
 *
 * <p>Measures every second the JVM's heap usage, maximum heap and an iteration count and reports
 * those as 'jvm.heap.used', 'jvm.heap.max' and 'iterations' metrics. In this simple example, we use
 * the measure name as the metric name because we only have one view on each of those measures.
 *
 * <p>This program runs forever until ^C-ed.
 */
public class SignalFxStatsExample {

  private static final MemoryMXBean MEMORY_MX_BEAN = ManagementFactory.getMemoryMXBean();

  private static final TagKey SOURCE_KEY = TagKey.create("sf_source");
  private static final Tagger TAGGER = Tags.getTagger();

  private static final MeasureDouble HEAP_USED = MeasureDouble
      .create("jvm.heap.used", "Used heap", "bytes");
  private static final MeasureDouble HEAP_MAX = MeasureDouble
      .create("jvm.heap.max", "Max heap", "bytes");
  private static final MeasureLong ITERATIONS = MeasureLong
      .create("iterations", "Execution iterations", "");

  static {
    Stats.getViewManager().registerView(View.create(View.Name.create(HEAP_USED.getName()),
        HEAP_USED.getDescription(), HEAP_USED, Aggregation.Mean.create(),
        Collections.singletonList(SOURCE_KEY), AggregationWindow.Cumulative.create()));

    Stats.getViewManager().registerView(View.create(View.Name.create(HEAP_MAX.getName()),
        HEAP_MAX.getDescription(), HEAP_MAX, Aggregation.Mean.create(),
        Collections.singletonList(SOURCE_KEY), AggregationWindow.Cumulative.create()));

    Stats.getViewManager().registerView(View.create(View.Name.create(ITERATIONS.getName()),
        ITERATIONS.getDescription(), ITERATIONS, Aggregation.Count.create(),
        Collections.singletonList(SOURCE_KEY), AggregationWindow.Cumulative.create()));
  }

  private static String getSourceName() throws UnknownHostException {
    String hostname = InetAddress.getLocalHost().getHostName();
    return String.format("%s/%s", hostname, SignalFxStatsExample.class.getSimpleName());
  }

  public static void main(String[] args) throws Exception {
    SignalFxStatsExporter.create();

    TagValue sourceTagValue = TagValue.create(getSourceName());
    TagContext sourceTagCtx = TAGGER.emptyBuilder().put(SOURCE_KEY, sourceTagValue).build();
    try (Scope ignored = TAGGER.withTagContext(sourceTagCtx)) {
      while (true) {
        MemoryUsage memoryUsage = MEMORY_MX_BEAN.getHeapMemoryUsage();
        long used = memoryUsage.getUsed();
        long max = memoryUsage.getMax();
        Stats.getStatsRecorder().newMeasureMap()
            .put(HEAP_USED, used)
            .put(HEAP_MAX, max)
            .put(ITERATIONS, 1).record();
        TimeUnit.SECONDS.sleep(1);
      }
    }
  }
}
