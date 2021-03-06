package com.codahale.metrics.graphite;

import org.junit.Before;
import org.junit.Test;

import javax.net.SocketFactory;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.failBecauseExceptionWasNotThrown;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

public class GraphiteTest {
    private final SocketFactory socketFactory = mock(SocketFactory.class);
    private final InetSocketAddress address = new InetSocketAddress("example.com", 1234);
    private final InetSocketAddress unresolvedAddress = InetSocketAddress.createUnresolved("example.com", 1234);
    private final Graphite graphite = new Graphite(address, socketFactory);

    private final Socket socket = mock(Socket.class);
    private final Socket unresolvedSocket = mock(Socket.class);
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @Before
    public void setUp() throws Exception {
        when(socket.getOutputStream()).thenReturn(output);

        when(socketFactory.createSocket(any(InetAddress.class),
                                        anyInt())).thenReturn(socket);

        when(socketFactory.createSocket(anyString(), anyInt())).thenReturn(unresolvedSocket);

    }

    @Test
    public void connectsToGraphite() throws Exception {
        graphite.connect();

        assertFalse(address.isUnresolved());  // this is the createSocket() method used when the name resovles
        verify(socketFactory).createSocket(address.getAddress(), address.getPort());

        assertNotNull(graphite.writer);
    }

    @Test
    public void connectsToGraphiteWithUnresolvedAddress() throws Exception {
        final Graphite graphite = new Graphite(unresolvedAddress, socketFactory);
        graphite.connect();

        verify(socketFactory).createSocket(any(InetAddress.class), eq(1234));

        assertNotNull(graphite.writer);
    }

    @Test
    public void measuresFailures() throws Exception {
        assertThat(graphite.getFailures())
                .isZero();
    }

    @Test
    public void disconnectsFromGraphite() throws Exception {
        graphite.connect();
        graphite.close();

        verify(socket).close();
    }

    @Test
    public void doesNotAllowDoubleConnections() throws Exception {
        graphite.connect();
        try {
            graphite.connect();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage())
                    .isEqualTo("Already connected");
        }
    }

    @Test
    public void writesValuesToGraphite() throws Exception {
        graphite.connect();
        graphite.send("name", "value", 100);

        assertThat(output.toString())
                .isEqualTo("name value 100\n");
    }

    @Test
    public void sanitizesNames() throws Exception {
        graphite.connect();
        graphite.send("name woo", "value", 100);

        assertThat(output.toString())
                .isEqualTo("name-woo value 100\n");
    }

    @Test
    public void sanitizesValues() throws Exception {
        graphite.connect();
        graphite.send("name", "value woo", 100);

        assertThat(output.toString())
                .isEqualTo("name value-woo 100\n");
    }
}
