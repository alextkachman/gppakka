/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mbte.akka

import akka.actor.UntypedActor
import akka.config.Supervision
import java.util.concurrent.CountDownLatch
import static akka.config.Supervision.*
import akka.config.Supervision.Supervise
import akka.config.Supervision.OneForOneStrategy
import akka.config.Supervision.SupervisorConfig
import akka.config.Supervision.Server
import akka.actor.SupervisorFactory
import akka.actor.Supervisor
import akka.actor.ActorRef

@Typed
@Use(GppAkka)
class AkkaTest extends GroovyShellTestCase {
    void testUntypedActorSimple () {
        def collected = []
        CountDownLatch cdl = [2]
        def actorRef = actorOf{{ msg ->
            collected << msg
            cdl.countDown()
        }}.start()

        actorRef << "Hello, world!" << "Bye-bye"

        cdl.await()
        actorRef.stop()
        assert collected == ["Hello, world!", "Bye-bye"]
    }

    void testUntypedActor () {
        def collected = []
        CountDownLatch cdl = [3]
        def actorRef = actorOf{{ msg ->
            preStart: {
                context << "preStart"
            }

            postStop: {
                collected << "postStop"
            }

            collected << msg
            cdl.countDown()
        }}.start()

        actorRef << "Hello, world!" << "Bye-bye"

        cdl.await()
        actorRef.stop()
        assert collected == ["preStart", "Hello, world!", "Bye-bye", "postStop"]
    }

    void testSupervisor () {
        def supervisor = actorOf{{ _ ->
            preStart: {
                @Field ActorRef ping, pong, printer

                printer = startLink {{ msg ->
                    preStart: {
                        println "------ START -----"
                    }
                    postStop: {
                        println "------ STOP -----"
                    }

                    println msg
                }}
                ping = startLink {{ msg ->
                    @Field counter = 0
                    printer << "ping: $msg ${counter++}"
                    pong << "PONG"
                }}
                pong = startLink {{ msg ->
                    @Field counter = 0
                    printer << "ping: $msg ${counter++}"
                    ping << "PING"
                }}

                ping << 'PING'
            }

            postStop: {
                stopLinked(ping)
                stopLinked(pong)
                stopLinked(printer)
            }
        }}
        supervisor.start()
        Thread.sleep 10000
        supervisor.stop ()
    }
}
