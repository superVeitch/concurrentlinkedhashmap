/*
 * Copyright 2011 Benjamin Manes
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
package com.googlecode.concurrentlinkedhashmap;

import static com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.BUFFER_THRESHOLD;
import static com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.MAXIMUM_BUFFER_SIZE;
import static com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.MAXIMUM_CAPACITY;
import static com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.bufferIndex;
import static com.googlecode.concurrentlinkedhashmap.IsEmptyCollection.emptyCollection;
import static com.googlecode.concurrentlinkedhashmap.IsEmptyMap.emptyMap;
import static com.googlecode.concurrentlinkedhashmap.IsValidState.valid;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.DrainStatus;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Task;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A unit-test for the page replacement algorithm and its public methods.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
@Test(groups = "development")
public final class EvictionTest extends BaseTest {

  @Override
  protected int capacity() {
    return 100;
  }

  /* ---------------- Ascending KeySet -------------- */

  @Test(dataProvider = "warmedMap")
  public void ascendingKeySet(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    warmUp(expected, 0, capacity());

    assertThat(map.ascendingKeySet(), is(equalTo(expected.keySet())));
  }

  @Test(dataProvider = "warmedMap")
  public void ascendingKeySet_snapshot(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    warmUp(expected, 0, capacity());

    Set<Integer> original = map.ascendingKeySet();
    map.put(capacity(), -capacity());

    assertThat(original, is(equalTo(expected.keySet())));
  }

  @Test(dataProvider = "warmedMap")
  public void ascendingKeySetWithLimit_greaterThan(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    warmUp(expected, 0, capacity() / 2);

    assertThat(map.ascendingKeySetWithLimit(capacity() / 2), is(equalTo(expected.keySet())));
  }

  @Test(dataProvider = "warmedMap")
  public void ascendingKeySetWithLimit_lessThan(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    warmUp(expected, 0, capacity());

    assertThat(map.ascendingKeySetWithLimit(capacity() * 2), is(equalTo(expected.keySet())));
  }

  @Test(dataProvider = "warmedMap")
  public void ascendingKeySetWithLimit_snapshot(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    warmUp(expected, 0, capacity() / 2);

    Set<Integer> original = map.ascendingKeySetWithLimit(capacity() / 2);
    map.put(capacity(), -capacity());

    assertThat(original, is(equalTo(expected.keySet())));
  }

  @Test(dataProvider = "warmedMap")
  public void ascendingKeySetWithLimit_zero(ConcurrentLinkedHashMap<Integer, Integer> map) {
    assertThat(map.ascendingKeySetWithLimit(0), is(emptyCollection()));
  }

  @Test(dataProvider = "warmedMap", expectedExceptions = IllegalArgumentException.class)
  public void ascendingKeySetWithLimit_negative(ConcurrentLinkedHashMap<Integer, Integer> map) {
    map.ascendingKeySetWithLimit(-1);
  }

  /* ---------------- Descending KeySet -------------- */

  @Test(dataProvider = "warmedMap")
  public void descendingKeySet(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Set<Integer> expected = new LinkedHashSet<Integer>();
    for (int i = capacity() - 1; i >= 0; i--) {
      expected.add(i);
    }
    assertThat(map.descendingKeySet(), is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void descendingKeySet_snapshot(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Set<Integer> expected = new LinkedHashSet<Integer>();
    for (int i = capacity() - 1; i >= 0; i--) {
      expected.add(i);
    }

    Set<Integer> original = map.descendingKeySet();
    map.put(capacity(), -capacity());

    assertThat(original, is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void descendingKeySetWithLimit_greaterThan(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Set<Integer> expected = new LinkedHashSet<Integer>();
    for (int i = capacity() - 1; i >= capacity() / 2; i--) {
      expected.add(i);
    }
    assertThat(map.descendingKeySetWithLimit(capacity() / 2), is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void descendingKeySetWithLimit_lessThan(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Set<Integer> expected = new LinkedHashSet<Integer>();
    for (int i = capacity() - 1; i >= 0; i--) {
      expected.add(i);
    }
    assertThat(map.descendingKeySetWithLimit(capacity() * 2), is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void descendingKeySetWithLimit_snapshot(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Set<Integer> expected = new LinkedHashSet<Integer>();
    for (int i = capacity() - 1; i >= capacity() / 2; i--) {
      expected.add(i);
    }

    Set<Integer> original = map.descendingKeySetWithLimit(capacity() / 2);
    map.put(capacity(), -capacity());

    assertThat(original, is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void descendingKeySetWithLimit_zero(ConcurrentLinkedHashMap<Integer, Integer> map) {
    assertThat(map.descendingKeySetWithLimit(0), is(emptyCollection()));
  }

  @Test(dataProvider = "warmedMap", expectedExceptions = IllegalArgumentException.class)
  public void descendingKeySetWithLimit_negative(ConcurrentLinkedHashMap<Integer, Integer> map) {
    map.descendingKeySetWithLimit(-1);
  }

  /* ---------------- Ascending Map -------------- */

  @Test(dataProvider = "warmedMap")
  public void ascendingMap(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    warmUp(expected, 0, capacity());

    assertThat(map.ascendingMap(), is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void ascendingMap_snapshot(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    warmUp(expected, 0, capacity());

    Map<Integer, Integer> original = map.ascendingMap();
    map.put(capacity(), -capacity());

    assertThat(original, is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void ascendingMapWithLimit_greaterThan(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    warmUp(expected, 0, capacity() / 2);

    assertThat(map.ascendingMapWithLimit(capacity() / 2), is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void ascendingMapWithLimit_lessThan(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    warmUp(expected, 0, capacity());

    assertThat(map.ascendingMapWithLimit(capacity() * 2), is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void ascendingMapWithLimit_snapshot(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    warmUp(expected, 0, capacity() / 2);

    Map<Integer, Integer> original = map.ascendingMapWithLimit(capacity() / 2);
    map.put(capacity(), -capacity());

    assertThat(original, is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void ascendingMapWithLimit_zero(ConcurrentLinkedHashMap<Integer, Integer> map) {
    assertThat(map.ascendingMapWithLimit(0), is(emptyMap()));
  }

  @Test(dataProvider = "warmedMap", expectedExceptions = IllegalArgumentException.class)
  public void ascendingMapWithLimit_negative(ConcurrentLinkedHashMap<Integer, Integer> map) {
    map.ascendingMapWithLimit(-1);
  }

  /* ---------------- Descending Map -------------- */

  @Test(dataProvider = "warmedMap")
  public void descendingMap(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    for (int i = capacity() - 1; i >= 0; i--) {
      expected.put(i, -i);
    }
    assertThat(map.descendingMap(), is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void descendingMap_snapshot(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    for (int i = capacity() - 1; i >= 0; i--) {
      expected.put(i, -i);
    }

    Map<Integer, Integer> original = map.descendingMap();
    map.put(capacity(), -capacity());

    assertThat(original, is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void descendingMapWithLimit_greaterThan(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    for (int i = capacity() - 1; i >= capacity() / 2; i--) {
      expected.put(i, -i);
    }
    assertThat(map.descendingMapWithLimit(capacity() / 2), is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void descendingMapWithLimit_lessThan(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    for (int i = capacity() - 1; i >= 0; i--) {
      expected.put(i, -i);
    }
    assertThat(map.descendingMapWithLimit(capacity() * 2), is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void descendingMapWithLimit_snapshot(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = new LinkedHashMap<Integer, Integer>();
    for (int i = capacity() - 1; i >= capacity() / 2; i--) {
      expected.put(i, -i);
    }

    Map<Integer, Integer> original = map.descendingMapWithLimit(capacity() / 2);
    map.put(capacity(), -capacity());

    assertThat(original, is(equalTo(expected)));
  }

  @Test(dataProvider = "warmedMap")
  public void descendingMapWithLimit_zero(ConcurrentLinkedHashMap<Integer, Integer> map) {
    assertThat(map.descendingMapWithLimit(0), is(emptyMap()));
  }

  @Test(dataProvider = "warmedMap", expectedExceptions = IllegalArgumentException.class)
  public void descendingMapWithLimit_negative(ConcurrentLinkedHashMap<Integer, Integer> map) {
    map.descendingMapWithLimit(-1);
  }

  /* ---------------- Capacity -------------- */

  @Test(dataProvider = "warmedMap")
  public void capacity_increase(ConcurrentLinkedHashMap<Integer, Integer> map) {
    Map<Integer, Integer> expected = ImmutableMap.copyOf(newWarmedMap());
    int newMaxCapacity = 2 * capacity();

    map.setCapacity(newMaxCapacity);
    assertThat(map, is(equalTo(expected)));
    assertThat(map.capacity(), is(equalTo(newMaxCapacity)));
  }

  @Test(dataProvider = "warmedMap")
  public void capacity_increaseToMaximum(ConcurrentLinkedHashMap<Integer, Integer> map) {
    map.setCapacity(MAXIMUM_CAPACITY);
    assertThat(map.capacity(), is(equalTo(MAXIMUM_CAPACITY)));
  }

  @Test(dataProvider = "warmedMap")
  public void capacity_increaseAboveMaximum(ConcurrentLinkedHashMap<Integer, Integer> map) {
    map.setCapacity(MAXIMUM_CAPACITY + 1);
    assertThat(map.capacity(), is(equalTo(MAXIMUM_CAPACITY)));
  }

  @Test
  public void capacity_decrease() {
    checkDecreasedCapacity(capacity() / 2);
  }

  @Test
  public void capacity_decreaseToMinimum() {
    checkDecreasedCapacity(0);
  }

  private void checkDecreasedCapacity(int newMaxCapacity) {
    CollectingListener<Integer, Integer> listener = new CollectingListener<Integer, Integer>();
    ConcurrentLinkedHashMap<Integer, Integer> map = new Builder<Integer, Integer>()
        .maximumWeightedCapacity(capacity())
        .listener(listener)
        .build();
    warmUp(map, 0, capacity());
    map.setCapacity(newMaxCapacity);

    assertThat(map, is(valid()));
    assertThat(map.size(), is(equalTo(newMaxCapacity)));
    assertThat(map.capacity(), is(equalTo(newMaxCapacity)));
    assertThat(listener.evicted, hasSize(capacity() - newMaxCapacity));
  }

  @Test(dataProvider = "warmedMap", expectedExceptions = IllegalArgumentException.class)
  public void capacity_decreaseBelowMinimum(ConcurrentLinkedHashMap<Integer, Integer> map) {
    try {
      map.setCapacity(-1);
    } finally {
      assertThat(map.capacity(), is(equalTo(capacity())));
    }
  }

  /* ---------------- Eviction -------------- */

  @Test(dataProvider = "builder", expectedExceptions = IllegalStateException.class)
  public void evictionListener_fails(Builder<Integer, Integer> builder) {
    ConcurrentLinkedHashMap<Integer, Integer> map = builder
        .listener(new EvictionListener<Integer, Integer>() {
          @Override public void onEviction(Integer key, Integer value) {
            throw new IllegalStateException();
          }
        })
        .maximumWeightedCapacity(0)
        .build();
    try {
      warmUp(map, 0, capacity());
    } finally {
      assertThat(map, is(valid()));
    }
  }

  @Test(dataProvider = "collectingListener")
  public void evict_alwaysDiscard(CollectingListener<Integer, Integer> listener) {
    ConcurrentLinkedHashMap<Integer, Integer> map = new Builder<Integer, Integer>()
        .maximumWeightedCapacity(0)
        .listener(listener)
        .build();
    warmUp(map, 0, 100);

    assertThat(map, is(valid()));
    assertThat(listener.evicted, hasSize(100));
  }

  @Test(dataProvider = "collectingListener")
  public void evict(CollectingListener<Integer, Integer> listener) {
    ConcurrentLinkedHashMap<Integer, Integer> map = new Builder<Integer, Integer>()
        .maximumWeightedCapacity(10)
        .listener(listener)
        .build();
    warmUp(map, 0, 20);

    assertThat(map, is(valid()));
    assertThat(map.size(), is(10));
    assertThat(map.weightedSize(), is(10));
    assertThat(listener.evicted, hasSize(10));
  }

  @Test(dataProvider = "builder")
  public void evict_weighted(Builder<Integer, Collection<Integer>> builder) {
    ConcurrentLinkedHashMap<Integer, Collection<Integer>> map = builder
        .weigher(Weighers.<Integer>collection())
        .maximumWeightedCapacity(10)
        .build();

    map.put(1, asList(1, 2));
    map.put(2, asList(3, 4, 5, 6, 7));
    map.put(3, asList(8, 9, 10));
    assertThat(map.weightedSize(), is(10));

    // evict (1)
    map.put(4, asList(11));
    assertThat(map.containsKey(1), is(false));
    assertThat(map.weightedSize(), is(9));

    // evict (2, 3)
    map.put(5, asList(12, 13, 14, 15, 16, 17, 18, 19, 20));
    assertThat(map.weightedSize(), is(10));

    assertThat(map, is(valid()));
  }

  @Test(dataProvider = "builder")
  public void evict_lru(Builder<Integer, Integer> builder) {
    ConcurrentLinkedHashMap<Integer, Integer> map = builder
        .maximumWeightedCapacity(10)
        .build();
    warmUp(map, 0, 10);
    checkContainsInOrder(map, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    // re-order
    checkReorder(map, asList(0, 1, 2), 3, 4, 5, 6, 7, 8, 9, 0, 1, 2);

    // evict 3, 4, 5
    checkEvict(map, asList(10, 11, 12), 6, 7, 8, 9, 0, 1, 2, 10, 11, 12);

    // re-order
    checkReorder(map, asList(6, 7, 8), 9, 0, 1, 2, 10, 11, 12, 6, 7, 8);

    // evict 9, 0, 1
    checkEvict(map, asList(13, 14, 15), 2, 10, 11, 12, 6, 7, 8, 13, 14, 15);

    assertThat(map, is(valid()));
  }

  private void checkReorder(ConcurrentLinkedHashMap<Integer, Integer> map,
      List<Integer> keys, Integer... expect) {
    for (int i : keys) {
      map.get(i);
    }
    checkContainsInOrder(map, expect);
  }

  private void checkEvict(ConcurrentLinkedHashMap<Integer, Integer> map,
      List<Integer> keys, Integer... expect) {
    for (int i : keys) {
      map.put(i, i);
    }
    checkContainsInOrder(map, expect);
  }

  private void checkContainsInOrder(ConcurrentLinkedHashMap<Integer, Integer> map,
      Integer... expect) {
    map.tryToDrainBuffers(false);
    List<Integer> evictionList = Lists.newArrayList();
    for (ConcurrentLinkedHashMap<Integer, Integer>.Node node : map.evictionDeque) {
      evictionList.add(node.key);
    }
    assertThat(map.size(), is(equalTo(expect.length)));
    assertThat(map.keySet(), containsInAnyOrder(expect));
    assertThat(evictionList, is(equalTo(asList(expect))));
  }

  @Test(dataProvider = "warmedMap")
  public void updateRecency_onGet(final ConcurrentLinkedHashMap<Integer, Integer> map) {
    final ConcurrentLinkedHashMap<Integer, Integer>.Node first = map.evictionDeque.peek();
    updateRecency(map, new Runnable() {
      @Override public void run() {
        map.get(first.key);
      }
    });
  }

  @Test(dataProvider = "warmedMap")
  public void updateRecency_onPutIfAbsent(final ConcurrentLinkedHashMap<Integer, Integer> map) {
    final ConcurrentLinkedHashMap<Integer, Integer>.Node first = map.evictionDeque.peek();
    updateRecency(map, new Runnable() {
      @Override public void run() {
        map.putIfAbsent(first.key, first.key);
      }
    });
  }

  @Test(dataProvider = "warmedMap")
  public void updateRecency_onPut(final ConcurrentLinkedHashMap<Integer, Integer> map) {
    final ConcurrentLinkedHashMap<Integer, Integer>.Node first = map.evictionDeque.peek();
    updateRecency(map, new Runnable() {
      @Override public void run() {
        map.put(first.key, first.key);
      }
    });
  }

  @Test(dataProvider = "warmedMap")
  public void updateRecency_onReplace(final ConcurrentLinkedHashMap<Integer, Integer> map) {
    final ConcurrentLinkedHashMap<Integer, Integer>.Node first = map.evictionDeque.peek();
    updateRecency(map, new Runnable() {
      @Override public void run() {
        map.replace(first.key, first.key);
      }
    });
  }

  @Test(dataProvider = "warmedMap")
  public void updateRecency_onReplaceConditionally(
      final ConcurrentLinkedHashMap<Integer, Integer> map) {
    final ConcurrentLinkedHashMap<Integer, Integer>.Node first = map.evictionDeque.peek();
    updateRecency(map, new Runnable() {
      @Override public void run() {
        map.replace(first.key, first.key, first.key);
      }
    });
  }

  private void updateRecency(ConcurrentLinkedHashMap<?, ?> map, Runnable operation) {
    ConcurrentLinkedHashMap<?, ?>.Node first = map.evictionDeque.peek();

    operation.run();
    map.drainBuffers();

    assertThat(map.evictionDeque.peekFirst(), is(not(first)));
    assertThat(map.evictionDeque.peekLast(), is(first));
    assertThat(map, is(valid()));
  }

  @Test(dataProvider = "guardedMap")
  public void applyInRecencyOrder(final ConcurrentLinkedHashMap<Integer, Integer> map)
      throws InterruptedException {
    final AtomicInteger tasks = new AtomicInteger();
    final AtomicInteger executed = new AtomicInteger();
    final class FakeTask extends DummyTask {
      final int id = tasks.getAndIncrement();

      FakeTask() {
        super(map.nextOrdering());
      }
      @Override public void run() {
        assertThat(id, is(executed.getAndIncrement()));
      }
      @Override
      public boolean isWrite() {
        return false;
      }
    }
    final BlockingQueue<Object> ping = new SynchronousQueue<Object>();
    final BlockingQueue<Object> pong = new SynchronousQueue<Object>();

    // even tasks
    new Thread() {
      @Override public void run() {
        for (int i = 0; i < BUFFER_THRESHOLD; i++) {
          try {
            ping.take();
            map.afterCompletion(new FakeTask());
            pong.put(new Object());
          } catch (InterruptedException e) {
            Assert.fail();
          }
        }
      }
    }.start();

    // odd tasks
    for (int i = 0; i < BUFFER_THRESHOLD; i++) {
      ping.put(new Object());
      pong.take();

      map.afterCompletion(new FakeTask());
    }

    // force a drain
    map.tryToDrainBuffers(false);
    assertThat(executed.get(), is(equalTo(tasks.get())));
    assertThat(executed.get(), is(2 * BUFFER_THRESHOLD));
  }

  @Test(dataProvider = "guardedMap")
  public void exceedsMaximumBufferSize_onRead(ConcurrentLinkedHashMap<Integer, Integer> map) {
    int index = bufferIndex();
    map.bufferLengths.set(index, MAXIMUM_BUFFER_SIZE);

    final boolean[] ran = { false };
    map.afterCompletion(new DummyTask(map.nextOrdering()) {
      @Override public void run() {
        ran[0] = true;
      }
      @Override public boolean isWrite() {
        return false;
      }
    });
    assertThat(ran[0], is(false));
    assertThat(map.buffers[index].size(), is(0));
    map.bufferLengths.set(index, 0);
  }

  @Test(dataProvider = "guardedMap")
  public void exceedsMaximumBufferSize_onWrite(ConcurrentLinkedHashMap<Integer, Integer> map) {
    int index = bufferIndex();
    map.bufferLengths.set(index, MAXIMUM_BUFFER_SIZE);

    final boolean[] ran = { false };
    map.afterCompletion(new DummyTask(map.nextOrdering()) {
      @Override public void run() {
        ran[0] = true;
      }
      @Override public boolean isWrite() {
        return true;
      }
    });
    assertThat(ran[0], is(true));
    assertThat(map.buffers[index].size(), is(0));
    map.bufferLengths.set(index, 0);
  }

  @Test(dataProvider = "warmedMap")
  public void drain_recencyQueue(ConcurrentLinkedHashMap<Integer, Integer> map) {
    for (int i = 0; i < BUFFER_THRESHOLD; i++) {
      map.get(1);
    }
    int index = bufferIndex();
    assertThat(map.bufferLengths.get(index), is(equalTo(BUFFER_THRESHOLD)));
    map.get(1);
    assertThat(map.bufferLengths.get(index), is(equalTo(0)));
  }

  @Test(dataProvider = "guardedMap")
  public void drain_nonblocking(final ConcurrentLinkedHashMap<Integer, Integer> map)
      throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    Thread thread = new Thread() {
      @Override public void run() {
        map.drainStatus.set(DrainStatus.REQUIRED);
        map.tryToDrainBuffers(false);
        latch.countDown();
      }
    };
    map.evictionLock.lock();
    try {
      thread.start();
    } finally {
      map.evictionLock.unlock();
    }
    assertThat(latch.await(1, TimeUnit.SECONDS), is(true));
  }

  @Test(dataProvider = "guardedMap")
  public void drain_blocksClear(final ConcurrentLinkedHashMap<Integer, Integer> map)
      throws InterruptedException {
    checkDrainBlocks(map, new Runnable() {
      @Override public void run() {
        map.clear();
      }
    });
  }

  @Test(dataProvider = "guardedMap")
  public void drain_blocksAscendingKeySet(final ConcurrentLinkedHashMap<Integer, Integer> map)
      throws InterruptedException {
    checkDrainBlocks(map, new Runnable() {
      @Override public void run() {
        map.ascendingKeySet();
      }
    });
  }

  @Test(dataProvider = "guardedMap")
  public void drain_blocksDescendingKeySet(final ConcurrentLinkedHashMap<Integer, Integer> map)
      throws InterruptedException {
    checkDrainBlocks(map, new Runnable() {
      @Override public void run() {
        map.descendingKeySet();
      }
    });
  }

  @Test(dataProvider = "guardedMap")
  public void drain_blocksAscendingMap(final ConcurrentLinkedHashMap<Integer, Integer> map)
      throws InterruptedException {
    checkDrainBlocks(map, new Runnable() {
      @Override public void run() {
        map.ascendingMap();
      }
    });
  }

  @Test(dataProvider = "guardedMap")
  public void drain_blocksDescendingMap(final ConcurrentLinkedHashMap<Integer, Integer> map)
      throws InterruptedException {
    checkDrainBlocks(map, new Runnable() {
      @Override public void run() {
        map.descendingMap();
      }
    });
  }

  @Test(dataProvider = "guardedMap")
  public void drain_blocksCapacity(final ConcurrentLinkedHashMap<Integer, Integer> map)
      throws InterruptedException {
    checkDrainBlocks(map, new Runnable() {
      @Override public void run() {
        map.setCapacity(0);
      }
    });
  }

  void checkDrainBlocks(final ConcurrentLinkedHashMap<Integer, Integer> map, Runnable task)
      throws InterruptedException {
    final ReentrantLock lock = (ReentrantLock) map.evictionLock;
    final CountDownLatch begin = new CountDownLatch(1);
    final CountDownLatch end = new CountDownLatch(1);

    Thread thread = new Thread() {
      @Override public void run() {
        map.drainStatus.set(DrainStatus.REQUIRED);
        begin.countDown();
        map.clear();
        end.countDown();
      }
    };
    lock.lock();
    try {
      thread.start();
      begin.await();
      long endTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
      while (!lock.hasQueuedThread(thread) && (endTime > System.nanoTime())) { /* busy wait */ }
      assertThat(lock.hasQueuedThread(thread), is(true));
    } finally {
      lock.unlock();
    }
    assertThat(end.await(1, TimeUnit.SECONDS), is(true));
  }

  static abstract class DummyTask implements Task {
    final int order;
    Task task;

    DummyTask(int order) {
      this.order = order;
    }
    @Override public int getOrder() {
      return order;
    }
    @Override public Task getNext() {
      return null;
    }
    @Override public void setNext(Task task) {
      this.task = task;
    }
  }
}
