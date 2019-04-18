package ddejonge.bandana.exampleAgents;

import java.util.*;

import ddejonge.bandana.anac.ANACNegotiator;
import ddejonge.bandana.dbraneTactics.DBraneTactics;
import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DMZ;
import ddejonge.bandana.negoProtocol.DiplomacyNegoClient;
import ddejonge.bandana.negoProtocol.DiplomacyProposal;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import ddejonge.bandana.tools.Utilities;
import ddejonge.negoServer.Message;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.HLDOrder;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;


public class NaturalAlliesBot extends ANACNegotiator{


    /**
     * Main method to start the agent
     * @param args
     */

    public static void main(String[] args){

        NaturalAlliesBot myPlayer = new NaturalAlliesBot(args);
        myPlayer.run();

    }

    String botName;
    DBraneTactics dBraneTactics;
    // initialize mapping from each power to its natural allies
    private static Map<String, List<String>> naturalAllies = new HashMap<>();
    static {
        naturalAllies.put("FRA", new ArrayList<>(Arrays.asList("ENG", "GER", "ITA")));
        naturalAllies.put("RUS", new ArrayList<>(Arrays.asList("AUS", "ENG", "GER", "ITA", "TUR")));
        naturalAllies.put("AUS", new ArrayList<>(Arrays.asList("ITA", "RUS", "TUR")));
        naturalAllies.put("ITA", new ArrayList<>(Arrays.asList("AUS", "FRA", "RUS")));
        naturalAllies.put("TUR", new ArrayList<>(Arrays.asList("AUS", "RUS")));
        naturalAllies.put("ENG", new ArrayList<>(Arrays.asList("FRA", "GER", "RUS")));
        naturalAllies.put("GER", new ArrayList<>(Arrays.asList("ENG", "FRA", "ITA", "RUS")));
    }

    private boolean isFirstRound = true;
    private List<Power> currentCoallition = new ArrayList<>();

    //Constructor

    /**
     * Inheriting from ANACNegotiator calling it's constructor
     * and also initiating D-Brane's Tactics module
     * @param args
     */
    public NaturalAlliesBot(String[] args) {
        super(args);

        dBraneTactics = this.getTacticalModule();
        botName = "NaturalAlliesBot";


    }

    /**
     * This method is automatically called at the start of the game, after the 'game' field is set.
     *
     * It is called when the first NOW message is received from the game server.
     * The NOW message contains the current phase and the positions of all the units.
     *
     * You are allowed, but not required, to implement this method
     *
     */
    @Override
    public void start() {

        //You can use the logger to write stuff to the log file.
        //The location of the log file can be set through the command line option -log.
        //it is not necessary to call getLogger().enable() because this is already automatically done by the ANACNegotiator class.

        boolean printLog = true;
        //if set to true the text will be written to file, as well as printed to the standard output stream.
        //If set to false it will only be written to file.

        this.getLogger().logln("game is starting! let's go " + this.botName + "! good luck!", printLog);
        String myPowerName = this.me.getName();
        this.getLogger().logln("" + this.botName + " is playing as " + myPowerName, printLog);

    }

    /**
     * This is where the magic happens!
     * Implementing the negotiations protocol!
     *
     * @param negotiationDeadline - maximum time allowed for negotiation procedure
     */

    @Override
    public void negotiate(long negotiationDeadline) {
    }

    /**
     * Each round, after each power has submitted its orders, this method is called several times:
     * once for each order submitted by any other power.
     *
     *
     */
    @Override
    public void receivedOrder(Order arg0) {
        // TODO Auto-generated method stub

    }

    private ArrayList<BasicDeal> getFirstRoundDealsOffer(){
        // initiate empty list of deals to be filled later
        ArrayList<BasicDeal> dealsToOffer = new ArrayList<>();

        // get names of all my natural allies
        List<String> myNaturalAlliesNames = NaturalAlliesBot.naturalAllies.get(this.me.getName());

        // offer to my natural allies a deal that includes joint DMZs
        List<Power> allPowers = this.game.getPowers();
        List<DMZ> demilitarizedZones = new ArrayList<>();

        for(Power power: allPowers) {
            String powerName = power.getName();

            if (myNaturalAlliesNames.contains(powerName)) { // only offer to natural allies
                List<Power> currentAllyForDeal = new ArrayList<>(Arrays.asList(power));
                demilitarizedZones.add(new DMZ(game.getYear(), game.getPhase(), currentAllyForDeal, me.getOwnedSCs()));

                List<Power> myPowerForDeal = new ArrayList<>(Arrays.asList(this.me));
                demilitarizedZones.add(new DMZ(game.getYear(), game.getPhase(), myPowerForDeal, power.getOwnedSCs()));

                List<OrderCommitment> emptyOrderCommitments = new ArrayList<>();
                BasicDeal deal = new BasicDeal(emptyOrderCommitments, demilitarizedZones);
                dealsToOffer.add(deal);
            }
        }
        return dealsToOffer;
    }
}
