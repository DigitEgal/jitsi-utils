/*
 * Copyright @ 2018 - present 8x8, Inc.
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
package org.jitsi.utils.stats

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jitsi.utils.OrderedJsonObject
import java.lang.IllegalArgumentException

class BucketStatsTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        context("adding stats") {
            val bucketStats = BucketStats(longArrayOf(2, 5, 200, 999), "_delay_ms", " ms")

            should("calculate the average correctly") {
                repeat(100) { bucketStats.addValue(1) }
                repeat(100) { bucketStats.addValue(5) }
                bucketStats.snapshot.average shouldBe 3.0
                bucketStats.toJson()["average_delay_ms"] shouldBe 3.0
            }

            should("calculate the max correctly") {
                repeat(100) { bucketStats.addValue(1) }
                repeat(100) { bucketStats.addValue(5) }
                bucketStats.addValue(100)
                bucketStats.snapshot.maxValue shouldBe 100
                bucketStats.toJson()["max_delay_ms"] shouldBe 100
            }

            should("export the buckets correctly to json") {
                repeat(100) { bucketStats.addValue(1) }
                repeat(100) { bucketStats.addValue(5) }
                bucketStats.addValue(150)
                bucketStats.addValue(1500)
                val bucketsJson = bucketStats.toJson()["buckets"]
                bucketsJson.shouldBeInstanceOf<OrderedJsonObject>()

                bucketsJson as OrderedJsonObject
                bucketsJson["<= 2 ms"] shouldBe 100
                bucketsJson["<= 5 ms"] shouldBe 100
                bucketsJson["<= 200 ms"] shouldBe 1
                bucketsJson["> 999 ms"] shouldBe 1
            }

            should("calculate p99 and p999 correctly") {
                bucketStats.snapshot.buckets.p99bound shouldBe -1
                bucketStats.snapshot.buckets.p999bound shouldBe -1

                repeat(200) { bucketStats.addValue(1) }
                bucketStats.snapshot.buckets.p99bound shouldBe 2
                bucketStats.snapshot.buckets.p999bound shouldBe -1

                repeat(800) { bucketStats.addValue(1) }
                bucketStats.snapshot.buckets.p999bound shouldBe 2

                bucketStats.addValue(5)
                bucketStats.addValue(5)
                bucketStats.snapshot.buckets.p99bound shouldBe 2
                bucketStats.snapshot.buckets.p999bound shouldBe 5

                repeat(2000) { bucketStats.addValue(1) }
                bucketStats.snapshot.buckets.p999bound shouldBe 2
            }
        }

        context("initializing with invalid thresholds should throw") {
            shouldThrow<IllegalArgumentException> {
                BucketStats(longArrayOf(2, 10, 5), "_delay_ms", " ms")
            }
        }
    }
}
