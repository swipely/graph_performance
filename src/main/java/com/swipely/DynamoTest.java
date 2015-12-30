package com.swipely;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.thinkaurelius.titan.core.EdgeLabel;
import com.thinkaurelius.titan.core.Multiplicity;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.VertexLabel;
import com.thinkaurelius.titan.core.schema.TitanManagement;

public class DynamoTest {

    private static final String TICKETS_EDGE_LABEL = "tickets";
    private static final String TICKET_VERTEX_LABEL = "ticket";

    public static void main(String[] args) throws InterruptedException {
        final Integer data_size = Integer.valueOf(100_000);
        final int edgesPerVertex = 10;
        //back of hand: at least two columns per vertex, and two more columns per edge
        //for the forward and reverse edges. With 1M edges, this means 2M columns for edges and
        //200k columns at least for vertices. Double for margin and end up with estimated
        //mutations of 5M mutations
        final Integer mutations = Integer.valueOf(5_000_000);

        final BaseConfiguration conf = new BaseConfiguration();
        conf.setProperty("storage.dynamodb.prefix", "t_crm_titan");
        conf.setProperty("schema.default", "none");
        conf.setProperty("storage.batch-loading", "true");

        conf.setProperty("storage.dynamodb.force-consistent-read", "false");
        conf.setProperty("storage.dynamodb.max-self-throttled-retries", "60");
        conf.setProperty("storage.dynamodb.control-plane-rate", "10");

        //32 partitions
        conf.setProperty("storage.dynamodb.stores.edgestore.capacity-read", "1");
        conf.setProperty("storage.dynamodb.stores.edgestore.capacity-write", "31968");
        conf.setProperty("storage.dynamodb.stores.edgestore.read-rate", "1");
        conf.setProperty("storage.dynamodb.stores.edgestore.write-rate", "31968");

//        conf.setProperty("storage.dynamodb.stores.graphindex.capacity-read", "12000");
//        conf.setProperty("storage.dynamodb.stores.graphindex.capacity-write", "12000");
//        conf.setProperty("storage.dynamodb.stores.graphindex.read-rate", "12000");
//        conf.setProperty("storage.dynamodb.stores.graphindex.write-rate", "12000");

        conf.setProperty("storage.dynamodb.stores.titan_ids.capacity-read", "100");
        conf.setProperty("storage.dynamodb.stores.titan_ids.capacity-write", "100");
        conf.setProperty("storage.dynamodb.stores.titan_ids.read-rate", "100");
        conf.setProperty("storage.dynamodb.stores.titan_ids.write-rate", "100");

        conf.setProperty("storage.dynamodb.stores.system_properties.capacity-read", "100");
        conf.setProperty("storage.dynamodb.stores.system_properties.capacity-write", "100");
        conf.setProperty("storage.dynamodb.stores.system_properties.read-rate", "100");
        conf.setProperty("storage.dynamodb.stores.system_properties.write-rate", "100");

        conf.setProperty("storage.dynamodb.client.connection-max", "1706");
        conf.setProperty("storage.dynamodb.client.retry-error-max", "0");
        conf.setProperty("storage.dynamodb.client.executor.core-pool-size", "1705");
        conf.setProperty("storage.dynamodb.client.executor.max-pool-size", "1705");
        conf.setProperty("storage.dynamodb.client.executor.max-queue-length", mutations.toString());

        conf.setProperty("storage.backend", "com.amazon.titan.diskstorage.dynamodb.DynamoDBStoreManager");
        conf.setProperty("storage.dynamodb.client.credentials.class-name",
                "com.amazonaws.auth.DefaultAWSCredentialsProviderChain");
        conf.setProperty("storage.dynamodb.client.credentials.constructor-args", "");
        conf.setProperty("storage.dynamodb.client.endpoint", "https://dynamodb.us-east-1.amazonaws.com");

        conf.setProperty("storage.buffer-size", mutations.toString());
//        conf.setProperty("storage.setup-wait", "300000"); //is this necessary?
        conf.setProperty("ids.block-size", data_size.toString());
        conf.setProperty("storage.write-time", "1 ms");
        conf.setProperty("storage.read-time", "1 ms");
        // conf.setProperty("cluster.partition", "true")
        // conf.setProperty("cluster.max-partitions", "32")
        conf.setProperty("ids.flush", "false");
        final TitanGraph g = TitanFactory.open(conf);

        final TitanManagement mgmt = g.openManagement();


        // Edge properties
        mgmt.makePropertyKey("weight").dataType(Float.class).make();
        mgmt.makePropertyKey("unit_price").dataType(Integer.class).make();
        mgmt.makePropertyKey("quantity").dataType(Integer.class).make();
        mgmt.makePropertyKey("item_sales").dataType(Integer.class).make();

        final PropertyKey dateKey = mgmt.makePropertyKey("date").dataType(String.class).make();
        final PropertyKey reconciledKey = mgmt.makePropertyKey("reconciled").dataType(Boolean.class).make();
        final PropertyKey classKey = mgmt.makePropertyKey("class").dataType(String.class).make();

        // Settlement and Authorization Properties
        final PropertyKey ledgerIdKey = mgmt.makePropertyKey("ledger_id").dataType(String.class).make();
        final PropertyKey reconciliationCodeKey = mgmt.makePropertyKey("reconciliation_code").dataType(String.class).make();
        final PropertyKey occurredAtKey = mgmt.makePropertyKey("occurred_at").dataType(String.class).make();

        // Vertex Properties
        final PropertyKey guestKey = mgmt.makePropertyKey("guest").dataType(String.class).make();
        final PropertyKey loyaltyKey = mgmt.makePropertyKey("loyalty_id").dataType(String.class).make();
        final PropertyKey otKey = mgmt.makePropertyKey("ot_id").dataType(String.class).make();
        final PropertyKey emailKey = mgmt.makePropertyKey("email").dataType(String.class).make();
        final PropertyKey guestNameKey = mgmt.makePropertyKey("guest_name").dataType(String.class).make();

        final PropertyKey storePrettyUrlKey = mgmt.makePropertyKey("store_pretty_url").dataType(String.class).make();
        final PropertyKey brandPrettyUrlKey = mgmt.makePropertyKey("brand_pretty_url").dataType(String.class).make();
        final PropertyKey merchantPrettyUrlKey = mgmt.makePropertyKey("merchant_pretty_url").dataType(String.class).make();
        final PropertyKey timezoneKey = mgmt.makePropertyKey("timezone_name").dataType(String.class).make();

        // Card token properties
        final PropertyKey cardTokenKey = mgmt.makePropertyKey("card_token").dataType(String.class).make();
        mgmt.makePropertyKey("card_type").dataType(String.class).make();
        mgmt.makePropertyKey("exp_date").dataType(String.class).make();
        mgmt.makePropertyKey("last_four").dataType(String.class).make();

        // Settlement and Authorization Properties
        mgmt.makePropertyKey("deposited_on").dataType(String.class).make();
        mgmt.makePropertyKey("fd_auth_date").dataType(String.class).make();
        mgmt.makePropertyKey("created_at_order").dataType(String.class).make();
        mgmt.makePropertyKey("financial_transactions_authorization_id").dataType(String.class).make();
        mgmt.makePropertyKey("authorization_price").dataType(Integer.class).make();
        mgmt.makePropertyKey("settlement_price").dataType(Integer.class).make();
        mgmt.makePropertyKey("reconciliation_order").dataType(String.class).make();
        mgmt.makePropertyKey("reject").dataType(Boolean.class).make();

        // Tickets
        mgmt.makePropertyKey("open_time").dataType(String.class).make();
        mgmt.makePropertyKey("open_hour").dataType(String.class).make();
        mgmt.makePropertyKey("close_time").dataType(String.class).make();
        mgmt.makePropertyKey("turn_time_seconds").dataType(Integer.class).make();
        mgmt.makePropertyKey("total_price").dataType(Integer.class).make();
        mgmt.makePropertyKey("tax").dataType(Integer.class).make();
        mgmt.makePropertyKey("covers").dataType(Integer.class).make();
        mgmt.makePropertyKey("table").dataType(String.class).make();

        final PropertyKey ticketIdKey = mgmt.makePropertyKey("ticket_id").dataType(String.class).make();
        final PropertyKey storeDayKey = mgmt.makePropertyKey("store_day").dataType(String.class).make();

        // Servers
        final PropertyKey serverFirstNameKey = mgmt.makePropertyKey("server_first_name").dataType(String.class).make();
        final PropertyKey serverLastNameKey = mgmt.makePropertyKey("server_last_name").dataType(String.class).make();
        final PropertyKey serverIdKey = mgmt.makePropertyKey("server_id").dataType(String.class).make();


        // Items
        final PropertyKey itemNameKey = mgmt.makePropertyKey("item_name").dataType(String.class).make();
        final PropertyKey itemIdKey = mgmt.makePropertyKey("item_id").dataType(String.class).make();

        // Categories
        final PropertyKey categoryKey = mgmt.makePropertyKey("category_name").dataType(String.class).make();

        // Payment
        mgmt.makePropertyKey("amount").dataType(Integer.class).make();
        final PropertyKey authCodeKey = mgmt.makePropertyKey("auth_code").dataType(String.class).make(); // also an edge property
        mgmt.makePropertyKey("cardholder_name").dataType(String.class).make();
        mgmt.makePropertyKey("payment_type").dataType(String.class).make();
        mgmt.makePropertyKey("tip").dataType(String.class).make();

        mgmt.makePropertyKey("guest_list_id").dataType(String.class).make();
        mgmt.makePropertyKey("guest_list_name").dataType(String.class).make();

        // final PropertyKey storePrettyUrlKey =
        // mgmt.makePropertyKey("store_pretty_url").dataType(String.class).make();
        // final PropertyKey dateKey =
        // mgmt.makePropertyKey("date").dataType(String.class).make();
        // final PropertyKey ticketIdKey =
        // mgmt.makePropertyKey("ticket_id").dataType(String.class).make();

        mgmt.makeEdgeLabel("members").multiplicity(Multiplicity.ONE2MANY).make();
        mgmt.makeEdgeLabel("guest_lists").multiplicity(Multiplicity.ONE2MANY).make();
        // Edge labels
        mgmt.makeEdgeLabel("has").multiplicity(Multiplicity.ONE2MANY).make();           // Customer has Profile, otCustomer has otReservation, Store has Tickets, etc
        mgmt.makeEdgeLabel("in").multiplicity(Multiplicity.MANY2ONE).make();           // Store in Brand, Brand in Group
        mgmt.makeEdgeLabel("timezone").multiplicity(Multiplicity.MANY2ONE).make();      // Store timezone Timezone
        mgmt.makeEdgeLabel("served").multiplicity(Multiplicity.SIMPLE).make();          // Server served Ticket
        final EdgeLabel withEdge = mgmt.makeEdgeLabel("with").multiplicity(Multiplicity.MANY2ONE).make();          // settlement with card_token, authorization with card_token
        mgmt.makeEdgeLabel("ordered").multiplicity(Multiplicity.SIMPLE).make();       // ticket ordered Item
        mgmt.makeEdgeLabel("tendered").multiplicity(Multiplicity.ONE2MANY).make();      // Customer tendered payment
        mgmt.makeEdgeLabel("authorized").multiplicity(Multiplicity.ONE2MANY).make();    // payment authorized authorization
        mgmt.makeEdgeLabel("settled").multiplicity(Multiplicity.ONE2MANY).make();       // payment settled settlement
        mgmt.makeEdgeLabel("for").multiplicity(Multiplicity.MANY2ONE).make();           // payment for ticket
        mgmt.makeEdgeLabel("enrolled").multiplicity(Multiplicity.ONE2MANY).make();      // LoyaltyMember enrolled CardToken
        final EdgeLabel guestsEdge = mgmt.makeEdgeLabel("guests").multiplicity(Multiplicity.ONE2MANY).make();          // store guests Guest
        final EdgeLabel ticketsEdge = mgmt.makeEdgeLabel("tickets").multiplicity(Multiplicity.ONE2MANY).make();          // store tickets Ticket, etc
        final EdgeLabel settlementsEdge = mgmt.makeEdgeLabel("settlements").multiplicity(Multiplicity.ONE2MANY).make(); // Store settlements settlement
        final EdgeLabel authorizationsEdge = mgmt.makeEdgeLabel("authorizations").multiplicity(Multiplicity.ONE2MANY).make();    // Store authorizations authorization
        final EdgeLabel paymentsEdge = mgmt.makeEdgeLabel("payments").multiplicity(Multiplicity.ONE2MANY).make();      // store payments payment
        final EdgeLabel itemsEdge = mgmt.makeEdgeLabel("items").multiplicity(Multiplicity.ONE2MANY).make();    // Store items item
        final EdgeLabel categoriesEdge = mgmt.makeEdgeLabel("categories").multiplicity(Multiplicity.ONE2MANY).make();    // Store categories category
        final EdgeLabel serversEdge = mgmt.makeEdgeLabel("servers").multiplicity(Multiplicity.ONE2MANY).make();    // Store items item

        // More than one Nick Name?
        mgmt.makeEdgeLabel("is_a").multiplicity(Multiplicity.MANY2ONE).make();          // Customer is_a otCustomer

        // final EdgeLabel ticketsEdge = mgmt.makeEdgeLabel(TICKETS_EDGE_LABEL).multiplicity(Multiplicity.MULTI).make();

        // this is for partitioned stores.
        // store = mgmt.makeVertexLabel("store").partition().make()
        // final VertexLabel storeLabel = mgmt.makeVertexLabel("store").make();
        final VertexLabel ticketLabel = mgmt.makeVertexLabel(TICKET_VERTEX_LABEL).make();

        // mgmt.buildIndex("byStorePrettyUrl",
        // Vertex.class).addKey(storePrettyUrlKey).unique().buildCompositeIndex();
        // mgmt.buildEdgeIndex(ticketsEdge,"byDateAndTicketId", Direction.OUT,
        // Order.decr,dateKey,ticketIdKey);

        mgmt.commit();
        System.out.println("Finished setting schema. Making data");

        final List<Vertex> tickets = new ArrayList<Vertex>();

        // lets use the same seed for consistent order
        final Random rand = new Random(1000);

        final int numVertices = data_size.intValue();
        long ts = System.currentTimeMillis();
        // start a new transaction
        final TitanTransaction tx = g.newTransaction();
        for (int i = 0; i < numVertices; i++) {
            tickets.add(tx.addVertex(TICKET_VERTEX_LABEL));
        }

        for (int i = 0; i < numVertices; i++) {
            final Vertex t1 = tickets.get(i);
            for (int j = 0; j < edgesPerVertex; j++) {
                //choose target avoiding self loops
                final int t2_i = (i + 1 + rand.nextInt(numVertices - 1)) % numVertices;
                final Vertex t2 = tickets.get(t2_i);
                t1.addEdge(TICKETS_EDGE_LABEL, t2);
            }
        }
        final long te = System.currentTimeMillis();
        System.out.println("committing edges");
        tx.commit();
        //need to use same tx scope if you are reusing vertex objects, otherwise
        //you would need to re-read the vertexes in the new transaction where you are creating
        //edges.
        final long tf = System.currentTimeMillis();
        System.out.println("Made " + data_size.toString() + " vertices and " + Integer.toString(edgesPerVertex) + " times as many edges in " + Double.toString((te - ts) / 1000.0) + " s and committed in "
                + Double.toString((tf - te) / 1000.0) + " s");

        System.out.println("Closing...");
        g.close();

        System.out.println("Tada - inserted ${data_size} tickets and ${edge_size} edges per ticket");
    }
}
