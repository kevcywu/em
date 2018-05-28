@echo off
TITLE Channel
COLOR 02
set CLASSPATH=.;dist\*
java -Xmx500m -Dnet.sf.odinms.listwz=false -Djavax.net.ssl.keyStore=channel.keystore -Djavax.net.ssl.keyStorePassword=passwd -Djavax.net.ssl.trustStore=channel.truststore -Djavax.net.ssl.trustStorePassword=passwd net.channel.ChannelServer
pause