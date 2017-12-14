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

package io.opencensus.exporter.stats.signalfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers.Dimension;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers.MetricType;
import io.opencensus.common.Duration;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.BucketBoundaries;
import io.opencensus.stats.View.AggregationWindow;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SignalFxSessionAdaptorTest {

  private static final Duration ONE_SECOND = Duration.create(1, 0);

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void checkMetricTypeFromAggregation() {
    assertNull(SignalFxSessionAdaptor.getMetricTypeForAggregation(null, null));
    assertNull(
        SignalFxSessionAdaptor.getMetricTypeForAggregation(
            null, AggregationWindow.Cumulative.create()));
    assertEquals(
        MetricType.GAUGE,
        SignalFxSessionAdaptor.getMetricTypeForAggregation(
            Aggregation.Mean.create(), AggregationWindow.Cumulative.create()));
    assertEquals(
        MetricType.GAUGE,
        SignalFxSessionAdaptor.getMetricTypeForAggregation(
            Aggregation.Mean.create(), AggregationWindow.Interval.create(ONE_SECOND)));
    assertEquals(
        MetricType.CUMULATIVE_COUNTER,
        SignalFxSessionAdaptor.getMetricTypeForAggregation(
            Aggregation.Count.create(), AggregationWindow.Cumulative.create()));
    assertEquals(
        MetricType.COUNTER,
        SignalFxSessionAdaptor.getMetricTypeForAggregation(
            Aggregation.Count.create(), AggregationWindow.Interval.create(ONE_SECOND)));
    assertEquals(
        MetricType.CUMULATIVE_COUNTER,
        SignalFxSessionAdaptor.getMetricTypeForAggregation(
            Aggregation.Sum.create(), AggregationWindow.Cumulative.create()));
    assertEquals(
        MetricType.COUNTER,
        SignalFxSessionAdaptor.getMetricTypeForAggregation(
            Aggregation.Sum.create(), AggregationWindow.Interval.create(ONE_SECOND)));
    assertNull(
        SignalFxSessionAdaptor.getMetricTypeForAggregation(Aggregation.Count.create(), null));
    assertNull(SignalFxSessionAdaptor.getMetricTypeForAggregation(Aggregation.Sum.create(), null));
    assertNull(
        SignalFxSessionAdaptor.getMetricTypeForAggregation(
            Aggregation.Distribution.create(
                BucketBoundaries.create(Collections.singletonList(3.14d))),
            AggregationWindow.Cumulative.create()));
  }

  @Test
  public void createDimensionsWithNonMatchingListSizes() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("don't have the same size");
    SignalFxSessionAdaptor.createDimensions(
        ImmutableList.of(TagKey.create("animal"), TagKey.create("color")),
        ImmutableList.of(TagValue.create("dog")));
  }

  @Test
  public void createDimensionsIgnoresEmptyValues() {
    List<Dimension> dimensions =
        Lists.newArrayList(
            SignalFxSessionAdaptor.createDimensions(
                ImmutableList.of(TagKey.create("animal"), TagKey.create("color")),
                ImmutableList.of(TagValue.create("dog"), TagValue.create(""))));
    assertEquals(1, dimensions.size());
    assertEquals("animal", dimensions.get(0).getKey());
    assertEquals("dog", dimensions.get(0).getValue());
  }

  @Test
  public void createDimension() {
    Dimension dimension =
        SignalFxSessionAdaptor.createDimension(TagKey.create("animal"), TagValue.create("dog"));
    assertEquals("animal", dimension.getKey());
    assertEquals("dog", dimension.getValue());
  }
}
