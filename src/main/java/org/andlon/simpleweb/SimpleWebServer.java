/**
 * Created by Andreas on 29.05.2014.
 */

package org.andlon.simpleweb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimpleWebServer {
    private ServerSocket m_server;
    private ConnectionPool m_pool;

    public SimpleWebServer(int port) throws IOException {
        m_pool = new ConnectionPool();
        m_server = new ServerSocket(port);
        m_server.setSoTimeout(1000);
    }

    public void run() throws IOException {
        while (m_server.isBound()) {
            try {
                Socket socket = m_server.accept();
                m_pool.add(Connection.fromSocket(socket));
            } catch(SocketTimeoutException e) { }
        }
    }

    static private class Connection {
        private Socket m_socket;
        private BufferedReader m_reader;
        private BufferedWriter m_writer;
        private long m_lastCommunication;
        private HttpRequestBuilder m_builder = new HttpRequestBuilder();

        private final int TIMEOUT_MILLISECONDS = 5000;

        private Connection(Socket socket) {
            m_socket = socket;
            m_lastCommunication = Instant.now().toEpochMilli();
            try {
                m_reader = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));
                m_writer = new BufferedWriter(new OutputStreamWriter(m_socket.getOutputStream()));
            } catch (Exception e) {

            }
        }

        static public Connection fromSocket(Socket socket) {
            return new Connection(socket);
        }

        public void disconnect() {
            try {
                m_socket.close();
            } catch (IOException e) { }
        }

        public boolean checkConnection() {
            long now = Instant.now().toEpochMilli();
            return (now - m_lastCommunication) < TIMEOUT_MILLISECONDS;
        }

        boolean poll() {
            try {
                if (m_reader.ready()) {

                    while (m_reader.ready()) {
                        char c = (char) m_reader.read();
                        m_builder.add(c);
                    }

                    m_lastCommunication = Instant.now().toEpochMilli();
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (MalformedRequestException e) {
                // Send 400 Bad Request here
                e.printStackTrace();
            }
            return false;
        }
    }

    private class ConnectionPool {
        private ScheduledExecutorService m_scheduler;
        private ArrayList<Connection> m_connections = new ArrayList<Connection>();
        private ArrayList<Connection> m_newConnections = new ArrayList<Connection>();
        private final Object newConnectionsLock = new Object();

        public ConnectionPool() {
            m_scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduleUpdate(1000);
        }

        public void add(Connection connection) {
            synchronized (newConnectionsLock) {
                m_newConnections.add(connection);
            }
        }

        private void scheduleUpdate(int microseconds) {
            m_scheduler.schedule(this::update, microseconds, TimeUnit.MICROSECONDS);
        }

        private void update() {
            synchronized(newConnectionsLock) {
                if (!m_newConnections.isEmpty()) {
                    m_connections.addAll(m_newConnections);
                    m_newConnections.clear();
                }
            }

            // Keep working until there is no more work, then schedule a timed update
            boolean idle;
            do {
                idle = true;

                // Iterate across connections, polling and removing disconnected clients
                Iterator<Connection> i = m_connections.iterator();
                while (i.hasNext()) {
                    Connection connection = i.next();
                    if (connection.poll()) {
                        idle = false;
                    } else if (!connection.checkConnection()) {
                        connection.disconnect();
                        i.remove();
                    }
                }
            } while (!idle);

            scheduleUpdate(1000);
        }
    }
}
