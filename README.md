# AS400 Anypoint Connector

[Connector description including destination service or application with]

# Mule supported versions
Mule 3.6.x, 3.7.x


# [Destination service or application name] supported versions


#Service or application supported modules



# Installation 
For beta connectors you can download the source code and build it with devkit to find it available on your local repository. Then you can add it to Studio…<TBD>

#Usage
Description
The IBM® AS/400® connector allows you to easily unlock the data in your IBM® AS/400®, reducing the time to market, total cost of ownership and integration complexity. The IBM® AS/400® connector works with native IBM® AS/400® objects (data queues and commands). The IBM® AS/400® connector is an operation-based connector, meaning when you add the connector to your flow, you configure a specific operation the connector is intended to perform. The connector supports the following operations:
Operation
Description
Read Data Queue (Message Source)
Perpetually listen for new messages arriving to specific data queue
Read Data Queue (Processor)
Read messages from specific data queue as part of Mule flow
Write to Data Queue
Write messages to data queue
Command Call
Execute IBM® AS/400® command call
Three common use cases cover the majority of IBM® AS/400® integration requirements:
	•	IBM® AS/400® process calls external function (i.e.) IBM® AS/400® process requests to convert transaction amount from one currency to another using real time exchange rate web service.
	•	External function calls IBM® AS/400® (i.e.) External function requests to retrieve product price from merchandizing system on IBM® AS/400®.
	•	External function needs to execute an IBM® AS/400® command line operation, for example create a new DB2 table, clear data queue, or call a custom program.


# Reporting Issues

We use GitHub:Issues for tracking issues with this connector. You can report new issues at this link http://github.com/mulesoft/as400/issues.