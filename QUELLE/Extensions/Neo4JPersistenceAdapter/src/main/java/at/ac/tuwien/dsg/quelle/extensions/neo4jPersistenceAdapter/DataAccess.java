/**
 * Copyright 2013 Technische Universitaet Wien (TUW), Distributed Systems Group
 * E184
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
package at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.visualization.graphviz.GraphvizWriter;
import org.neo4j.walk.Walker;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
@Service
public class DataAccess implements InitializingBean, DisposableBean {

    private EmbeddedGraphDatabase graphDatabaseService;

    public DataAccess(String databaseLocation) {
        graphDatabaseService = new EmbeddedGraphDatabase(databaseLocation);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDatabaseService.shutdown();
            }
        });
    }

    public EmbeddedGraphDatabase getGraphDatabaseService() {
        return graphDatabaseService;
    }

    public void startGUIServer() {
        final WrappingNeoServerBootstrapper srv = new WrappingNeoServerBootstrapper(graphDatabaseService);

        srv.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                srv.stop();
            }
        });
    }

    public void writeGraphAsGraphVis(String filePath) throws Exception {
        Transaction t = graphDatabaseService.beginTx();
        try {
            GraphvizWriter writer = new GraphvizWriter();
            OutputStream out = new FileOutputStream(filePath);

            writer.emit(out, Walker.fullGraph(graphDatabaseService));

            out.flush();
            out.close();

            t.success();
        } catch (IOException e) {

            t.failure();
        }
        t.finish();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
 
    }

    @Override
    public void destroy() throws Exception {
 
        graphDatabaseService.shutdown();
 
    }

    public void clear() {
        Transaction t = graphDatabaseService.beginTx();
        try {
            for (Node node : graphDatabaseService.getAllNodes()) {

                for (Relationship r : node.getRelationships(Direction.BOTH)) {
                    r.delete();
                }
                node.delete();
            }

            t.success();
        } catch (Exception e) {

        } finally {
            t.finish();
        }

    }
 

}
