@echo off
TITLE ~Clean Revision 988 Files~ Made by Rigged of RaGEZONE Forums (channel)
COLOR 02
set CLASSPATH=.;dist\*
java -Xmx500m -Dnet.sf.odinms.recvops=recvops.properties -Dnet.sf.odinms.sendops=sendops.properties -Dnet.sf.odinms.wzpath=wz\ -Dnet.sf.odinms.channel.config=channel.properties -Dnet.sf.odinms.listwz=true -Djavax.net.ssl.keyStore=channel.keystore -Djavax.net.ssl.keyStorePassword=passwd -Djavax.net.ssl.trustStore=channel.truststore -Djavax.net.ssl.trustStorePassword=passwd net.channel.ChannelServer
pause