/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ut.lulyfan.exrobot.ros;

import org.ros.concurrent.CancellableLoop;
import org.ros.internal.node.topic.SubscriberIdentifier;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.PublisherListener;

/**
 * A simple {@link Publisher} {@link NodeMain}.
 *
 * @author damonkohler@google.com (Damon Kohler)
 */
public abstract class Talker<T> extends AbstractNodeMain {
    public static final int LOOP = -1;  //表示循环发送模式

    private String topic_name;
    private String msgType;
    protected int sendCount;
    protected long gapTime = 1000;  //发送的间隔时间

    public Talker() {
        topic_name = "chatter";
    }

    public Talker(String topic, String msgType) {
        topic_name = topic;
        this.msgType = msgType;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("talker/" + topic_name);
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        System.out.println(topic_name + " publish onStart");
        final Publisher<T> p = connectedNode.newPublisher(topic_name, msgType);

        p.addListener(new PublisherListener<T>() {
            private boolean isInit;
            @Override
            public void onNewSubscriber(final Publisher<T> publisher, SubscriberIdentifier subscriberIdentifier) {
                // This CancellableLoop will be canceled automatically when the node shuts
                // down.
                System.out.println("onNewSubscriber");
                if (isInit)
                    return;

                isInit = true;
                connectedNode.executeCancellableLoop(new CancellableLoop() {

                    @Override
                    protected void loop() throws InterruptedException {
                        if (sendCount > 0 || sendCount == LOOP) {
                            T msg = publisher.newMessage();
                            editMsg(msg);
                            publisher.publish(msg);
                            System.out.println(topic_name + " publish msg");

                            if (sendCount != LOOP)
                                sendCount--;

                            Thread.sleep(gapTime);
                        }
                    }
                });
            }

            @Override
            public void onShutdown(Publisher<T> publisher) {

            }

            @Override
            public void onMasterRegistrationSuccess(Publisher<T> tPublisher) {

            }

            @Override
            public void onMasterRegistrationFailure(Publisher<T> tPublisher) {

            }

            @Override
            public void onMasterUnregistrationSuccess(Publisher<T> tPublisher) {

            }

            @Override
            public void onMasterUnregistrationFailure(Publisher<T> tPublisher) {

            }
        });

    }

    abstract protected void editMsg(T msg);
}
