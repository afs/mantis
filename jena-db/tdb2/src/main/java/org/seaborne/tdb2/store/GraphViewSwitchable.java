/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seaborne.tdb2.store;

import java.util.Map ;

import org.apache.jena.graph.Graph ;
import org.apache.jena.graph.Node ;
import org.apache.jena.shared.PrefixMapping ;
import org.apache.jena.shared.impl.PrefixMappingImpl ;
import org.apache.jena.sparql.core.DatasetPrefixStorage ;
import org.apache.jena.sparql.core.GraphView ;
import org.apache.jena.sparql.core.Quad ;

/** A GraphView that is sensitive to {@link DatasetGraphSwitchable} switching.
 */
public class GraphViewSwitchable extends GraphView {
    // Fixups for GraphView
    //   Prefixes.
    //   Transaction handler.
    // Long term - ensure that GraphView uses get() always, inc prefixes, transaction handlers
    
    // Factory style.
    public static GraphViewSwitchable createDefaultGraph(DatasetGraphSwitchable dsg)
    { return new GraphViewSwitchable(dsg, Quad.defaultGraphNodeGenerated) ; }
    
    public static GraphView createNamedGraph(DatasetGraphSwitchable dsg, Node graphIRI)
    { return new GraphViewSwitchable(dsg, graphIRI) ; }
    
    public static GraphViewSwitchable createUnionGraph(DatasetGraphSwitchable dsg)
    { return new GraphViewSwitchable(dsg, Quad.unionGraph) ; }
    
    private final DatasetGraphSwitchable dsgx;
    protected DatasetGraphSwitchable getx() { return dsgx; }
    
    protected GraphViewSwitchable(DatasetGraphSwitchable dsg, Node gn) {
        super(dsg, gn) ;
        this.dsgx = dsg;
    }

//    @Override
//    public TransactionHandler getTransactionHandler() {
//        // XXX Awiting for promote to be enabled.
//        return super.getTransactionHandler();
//    }
    
    @Override
    protected PrefixMapping createPrefixMapping() {
        Node gn = super.getGraphName();
        if ( gn == Quad.defaultGraphNodeGenerated )
            gn = null;
        if ( Quad.isUnionGraph(gn) ) {
            // Read-only wrapper would be better that a copy.
            PrefixMapping pmap = new PrefixMappingImpl();
            pmap.setNsPrefixes(prefixMapping(null));
            return pmap; 
        }
        return prefixMapping(gn);
    }
    
    /** Return the {@code DatasetGraphSwitchable} we are viewing. */
    @Override
    public DatasetGraphSwitchable getDataset() {
        return getx() ;
    }

    // TDB2 specific.
    // Does not cope with blank nodes.
    // A PrefixMapping sending operations via the switchable.
    // Long term, rework as PrefixMapping over PrefixMap over DatasetPrefixStorage
    private PrefixMapping prefixMapping(Node graphName) {
        
        String gn = (graphName == null) ? "" : graphName.getURI(); 
        
        return new PrefixMappingImpl() {
            
            DatasetPrefixStorage dps() {
                return ((DatasetGraphTDB)(getx().get())).getPrefixes();
            }
            
            Graph graph() {
                DatasetGraphTDB dsg = (DatasetGraphTDB)getx().get();
                if ( gn == null )
                    return dsg.getDefaultGraph();
                else
                    return dsg.getGraph(graphName);
            }
            
            PrefixMapping prefixMapping() {
                if ( gn == null )
                    return dps().getPrefixMapping();
                else
                    return dps().getPrefixMapping(gn); 
            }

            @Override
            protected void set(String prefix, String uri) {
                dps().insertPrefix(gn, prefix, uri);
                super.set(prefix, uri);
            }

            @Override
            protected String get(String prefix) {
                //Ignore. String x = super.get(prefix);
                // (Ignore more?)
                return dps().readPrefix(gn, prefix);
            }

            @Override
            protected void remove(String prefix) {
                dps().getPrefixMapping().removeNsPrefix(prefix);
                super.remove(prefix);
            }
            
            @Override
            public Map<String, String> getNsPrefixMap() {
                return prefixMapping().getNsPrefixMap();
                //return graph().getPrefixMapping().getNsPrefixMap();
            }
        };
    }
    
}