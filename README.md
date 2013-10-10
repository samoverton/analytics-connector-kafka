
**Apache Kafka Connector for Acunu Analytics**

Copyright (c) 2013 Acunu.

Requires Acunu Analytics >=v5.0, includes Apache Kafka 0.7.2 

To build the connector, first check `$JBIRD_HOME` points to your Acunu Analytics 
installation:

    export JBIRD_HOME=/usr/share/acunu/jbird/

You'll need the JARs that are supplied with Analytics, especially the Analytics
Connectors API, `analytics-connectors.jar`.
  
To build the connector, run:

    make
  
And to install the connector into the JBIRD_HOME/plugins directory:

    sudo make install
  
Or to install it somewhere else:

    PLUGINS_DIR=/my/path/to/plugins/ make install
  
You'll need to restart Analytics for it to pick up the new connector.

Then, create a Kafka ingester:

    CREATE INGESTER kafka USING 'com.acunu.analytics.kafka.KafkaIngester' 
      PROPERTIES groupid = 'group1', zk_connect = 'localhost:2181';

Note that Kafka properties that contain dots (.) need those replacing with 
underscores (_).

And create a flow that uses it (having created a table first):

    CREATE FLOW my_flow INGESTER kafka RECEIVER my_table 
      PROPERTIES topic = 'topic1';

This uses the default decoder, which expects Strings, bytes or chars to 
be sent, and will interpret JSON objects. If you're sending Kafka messages
in a particular format, can specify your own decoder:

    CREATE FLOW my_flow INGESTER kafka DECODER 'com.my.company.MyDecoder' 
      RECEIVER my_table PROPERTIES topic = 'topic1';

See the [Acunu documentation](http://www.acunu.com/documentation.html#%2Fv5.0%2Fdeveloper%2Fplugins.html) for more details about decoders. 

Once you've set up a flow, test it with a Kakfa consumer. Start Zookeeper, 
start the Kafka server, then start a consumer to send messages on the same
topic and group as you configured in the ingester and flow.

We welcome questions, suggestions, feedback, patches and problems reports. 

Please get in touch at http://support.acunu.com/

