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
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.HLDOrder;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.SUPMTOOrder;
import es.csic.iiia.fabregues.dip.orders.SUPOrder;


public class NaturalAlliesBotOldManyOptions extends ANACNegotiator{


    /**
     * Main method to start the agent
     * @param args
     */

    public static void main(String[] args){

        NaturalAlliesBotOldManyOptions myPlayer = new NaturalAlliesBotOldManyOptions(args);
        myPlayer.run();

    }

    public String botName;
    protected DBraneTactics dBraneTactics;
    // initialize mapping from each power to its natural allies
    protected static Map<String, List<String>> naturalAllies = new HashMap<>();
    static {
        naturalAllies.put("FRA", new ArrayList<>(Arrays.asList("ENG", "GER", "ITA")));
        naturalAllies.put("RUS", new ArrayList<>(Arrays.asList("AUS", "ENG", "GER", "ITA", "TUR")));
        naturalAllies.put("AUS", new ArrayList<>(Arrays.asList("ITA", "RUS", "TUR")));
        naturalAllies.put("ITA", new ArrayList<>(Arrays.asList("AUS", "FRA", "RUS")));
        naturalAllies.put("TUR", new ArrayList<>(Arrays.asList("AUS", "RUS")));
        naturalAllies.put("ENG", new ArrayList<>(Arrays.asList("FRA", "GER", "RUS")));
        naturalAllies.put("GER", new ArrayList<>(Arrays.asList("ENG", "FRA", "ITA", "RUS")));
    }

    protected boolean isFirstRound = true;
    protected List<Power> myNaturalAllies = new ArrayList<>();
    protected List<Power> additionalAlliePowers = new ArrayList<>();

    //Constructor

    /**
     * Inheriting from ANACNegotiator calling it's constructor
     * and also initiating D-Brane's Tactics module
     * @param args
     */
    public NaturalAlliesBotOldManyOptions(String[] args) {
        super(args);

        dBraneTactics = this.getTacticalModule();
        botName = "NaturalAlliesBotOldManyOptions";


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
        boolean startOfCurrentNegotiation = true;

        if (this.isFirstRound){
            // update natural allies array
            this.updateNaturalAllies();
        }

        // This loop repeats 2 steps
        // During the first step we try to find deals to propose to the other negotiators.
        // During the second step we handle any incoming messages.
        while (System.currentTimeMillis() < negotiationDeadline) {


            // STEP 1: Handle incoming messages

            //See if we have received any message from any of the other negotiators.
            // e.g. a new proposal or an acceptance of a proposal made earlier.
            while(hasMessage()) {

                //if yes, remove it from the message queue.
                Message receivedMessage = removeMessageFromQueue();

                // extract message properties and log the event
                DiplomacyProposal messageContent = (DiplomacyProposal) receivedMessage.getContent();
                String messageSender = receivedMessage.getSender();
                Power senderPower = this.game.getPower(messageSender);
                String messageType = receivedMessage.getPerformative();

                this.getLogger().logln("got message " + messageContent + " from " + messageSender, false);

                switch (messageType){
                    case DiplomacyNegoClient.ACCEPT:

                        // log received acceptance and content
                        this.getLogger().logln("" + this.botName + ".negotiate() Received acceptance from " + messageSender + ": " + messageContent, false);

                    case DiplomacyNegoClient.PROPOSE:
                        BasicDeal deal = (BasicDeal) messageContent.getProposedDeal();
                        // log proposal
                        this.getLogger().logln("" + this.botName + ".negotiate() Received proposal: " + messageContent, false);

                        // check if deal is not outdated and consistent with previous deals
                        if (this.checkProposedDealIsConsistentAndNotOutDated(deal)){

                            // for now we just accept any valid deals from our natural allies
                            if (this.checkIfPowerIsNaturalAlly(senderPower)){
                                this.acceptProposal(messageContent.getId());
                            }
                        }
                        break;

                    case DiplomacyNegoClient.CONFIRM:
                        // we pretty much do nothing here
                        // just log the confirmation
                        this.getLogger().logln( "" + this.botName + ".negotiate() Received confirmed from " + messageSender + ": " + messageContent, false);
                        break;

                    case DiplomacyNegoClient.REJECT:
                        // we don't do nothing here, not really interesting
                        break;

                    default:
                        // received a message of unhandled type - just log the event
                        this.getLogger().logln("" + this.botName + ".negotiate() Received a message of unhandled type: " + receivedMessage.getPerformative() + ". Message content: " + messageContent.toString(), false);

                }

            }

            // STEP 2:  try to find a proposal to make, and if we do find one, propose it.
            List<BasicDeal> dealsToPropose;

            // search for deals only one time per negotiation round
            if (startOfCurrentNegotiation){
                dealsToPropose = this.getDealsToOffer();

                // if deals were found - propose them
                if (dealsToPropose != null){
                    for (BasicDeal deal: dealsToPropose){
                        this.proposeDeal(deal);
                    }
                    startOfCurrentNegotiation = false;
                }
            }

            // Sleep for 250 milliseconds - let other negotiators time to propose deals
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {;}
        }

        this.getLogger().logln("" + this.botName + ".negotiate(): end of a negotiation round", true);

        // at the end of while loop
        // if it was the first round - update that it is over
        if (this.isFirstRound){
            this.getLogger().logln("" + this.botName + ".negotiate(): end of FIRST negotiation round", true);
            this.isFirstRound = false;
        }
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

    protected List<BasicDeal> getFirstRoundDealsOfferNaturalAllies(){

        this.getLogger().logln("" + this.botName + ": First round deals offers. Offering DMZ areas to Natural Allies", false);

        // initiate empty list of deals to be filled later
        ArrayList<BasicDeal> dealsToOffer = new ArrayList<>();

        // get names of all my natural allies
        List<String> myNaturalAlliesNames = NaturalAlliesBotOldManyOptions.naturalAllies.get(this.me.getName());

        this.getLogger().logln("" + this.botName + ": First round deals offers. My power is: " + this.me.getName() + " " +
                "My natural allies are: " + Arrays.toString(myNaturalAlliesNames.toArray()), true);

        // offer to my natural allies a deal that includes joint DMZs
        List<Power> allPowers = this.game.getPowers();

        for(Power power: allPowers) {
            List<DMZ> demilitarizedZones = new ArrayList<>();

            if (this.checkIfPowerIsNaturalAlly(power)) { // only offer to natural allies

                this.getLogger().logln("" + this.botName + ": First round deals offers. Offering DMZ to: " + power.getName(), true);

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

    protected List<BasicDeal> getPersonalDmzDealsOffers(List<Power> availablePowers) {
        // initiate empty list of deals to be filled later
        ArrayList<BasicDeal> dealsToOffer = new ArrayList<>();

        for (Power power : availablePowers) {
            List<DMZ> demilitarizedZones = new ArrayList<>();

            this.getLogger().logln("" + this.botName + ": First round deals offers. Offering DMZ to: " + power.getName(), true);

            List<Power> currentAllyForDeal = new ArrayList<>(Arrays.asList(power));
            demilitarizedZones.add(new DMZ(game.getYear(), game.getPhase(), currentAllyForDeal, me.getOwnedSCs()));

            List<Power> myPowerForDeal = new ArrayList<>(Arrays.asList(this.me));
            demilitarizedZones.add(new DMZ(game.getYear(), game.getPhase(), myPowerForDeal, power.getOwnedSCs()));

            List<OrderCommitment> emptyOrderCommitments = new ArrayList<>();
            BasicDeal deal = new BasicDeal(emptyOrderCommitments, demilitarizedZones);
            dealsToOffer.add(deal);
        }
        return dealsToOffer;
    }

    protected List<BasicDeal> getCoalitionDmzDealsOffer(List<Power> aliveCoalitionMembers){

        this.getLogger().logln("" + this.botName + ": Advanced round deals offers. Offering DMZ areas to coalition members", true);
        // initiate empty list of deals to be filled later
        ArrayList<BasicDeal> dmzDealsToOffer = new ArrayList<>();

        // make offers for all the coalition members to not attack each other

        for (Power currentMember: aliveCoalitionMembers) {
            List<DMZ> demilitarizedZones = new ArrayList<>();

            this.getLogger().logln("" + this.botName + ": Advanced round deals offers. Offering DMZ areas to the member: " + currentMember.getName(), true);

            // create a list of all other coalition members to participate in the deal
            ArrayList<Power> allOtherMembers = new ArrayList<>();

            // include our power in the list
            allOtherMembers.add(me);
            for (Power potentialMember: aliveCoalitionMembers) {

                // We want to make an offer including all the other coalition members except current one
                if (potentialMember != currentMember) {
                    allOtherMembers.add(potentialMember);
                }
            }

            // create a DMZ deal containing all alive member committing not to invade the current member SCs
            demilitarizedZones.add(new DMZ(game.getYear(), game.getPhase(), allOtherMembers, currentMember.getOwnedSCs()));
            List<OrderCommitment> emptyOrderCommitments = new ArrayList<>();
            BasicDeal deal = new BasicDeal(emptyOrderCommitments, demilitarizedZones);
            dmzDealsToOffer.add(deal);
        }



        return dmzDealsToOffer;
    }

    protected List<BasicDeal> getDealsToOffer() {
        List<BasicDeal> dealsToOffer = new ArrayList<>();
        List<BasicDeal> dmzDealsToOffer = new ArrayList<>();

        if (this.isFirstRound) {

            // get DMZ deals with natural allies
            dmzDealsToOffer.addAll(this.getPersonalDmzDealsOffers(this.myNaturalAllies));

        } else if (this.game.getYear() <= 1904){

            // get DMZ deals with alive natural allies
            List<Power> aliveCoalitionMembers = this.getAliveMembers(this.myNaturalAllies);

            if (aliveCoalitionMembers.size() > 1){
                // we cannot offer deals that include only ourselves
                // hence in case of only single natural ally alive we will offer him the personal DMZ deal
                dmzDealsToOffer.addAll(this.getPersonalDmzDealsOffers(aliveCoalitionMembers));
            }

            else{
                // if we have more than one we will offer the coalition DMZ offer
                dmzDealsToOffer.addAll(this.getCoalitionDmzDealsOffer(aliveCoalitionMembers));
            }
        }

        // add DMZ deals and support deals to current negotiation deals
        dealsToOffer.addAll(dmzDealsToOffer);

        // use D-Brane tactics module to get a plan of good orders subjected to current commitments
        Plan tacticPlan = this.dBraneTactics.determineBestPlan(game, this.me, this.getConfirmedDeals());
        List<Order> planOrders = tacticPlan.getMyOrders();
        List<BasicDeal> supportDealsToOffer = this.getDealsToSupportMoveAndHoldOrders(planOrders);

        // add DMZ support deals to current negotiation deals
        dealsToOffer.addAll(supportDealsToOffer);

        return dealsToOffer;

    }

    private void updateNaturalAllies(){
        List<String> myNaturalAlliesNames = NaturalAlliesBotOldManyOptions.naturalAllies.get(this.me.getName());

        for (String powerName: myNaturalAlliesNames){
            this.myNaturalAllies.add(this.game.getPower(powerName));
        }
    }

    protected List<Power> getAliveMembers(List<Power> potentialPowers){
        //Get the names of all the powers that are connected to the negotiation server and which have not been eliminated.
        List<Power> aliveNegotiatingPowers = this.getNegotiatingPowers();
        List<Power> aliveMembers = new ArrayList<>();

        for (Power power: potentialPowers){
            if (aliveNegotiatingPowers.contains(power)){
                aliveMembers.add(power);
            }
        }
        return aliveMembers;
    }

    protected void addAcceptedProposalMemberToCoalition(Power senderPower){
        // because we have a list of coalition member and not a set we will check if the power exists
        if (!this.additionalAlliePowers.contains(senderPower)){
            this.getLogger().logln("" + this.botName + ": Adding member to coalition: " + senderPower.getName(), true);
            this.additionalAlliePowers.add(senderPower);
        }
    }

    protected boolean checkIfPowerIsNaturalAlly(Power power){
        return this.myNaturalAllies.contains(power);
    }

    protected boolean checkIfPowerIsCurrentAlly(Power power){
        return this.additionalAlliePowers.contains(power);
    }

    protected boolean checkProposedDealIsConsistentAndNotOutDated(BasicDeal proposedDeal) {
        // check DMZ offers are not outdated
        for (DMZ dmz : proposedDeal.getDemilitarizedZones()) {

            // Sometimes we may receive messages too late, so we check if the proposal does not
            // refer to some round of the game that has already passed.
            // one offer is enough to eliminate the entire deal
            if (isHistory(dmz.getPhase(), dmz.getYear())) {
                return false;
            }
        }

        // check order commitments
        for(OrderCommitment orderCommitment : proposedDeal.getOrderCommitments()){

            // Sometimes we may receive messages too late, so we check if the proposal does not
            // refer to some round of the game that has already passed.
            // one offer is enough to eliminate the entire deal
            if(isHistory(orderCommitment.getPhase(), orderCommitment.getYear())){
                return false;
            }
        }

        // check offer is consistent with previous offers
        String consistencyReport;
        List<BasicDeal> commitments = new ArrayList<>();
        commitments.addAll(this.getConfirmedDeals());
        commitments.add(proposedDeal);
        consistencyReport = Utilities.testConsistency(game, commitments);

        // if we got up until here we know the offer is not outdated
        // so if it is consistent we say it is valid
        // the consistency report returns null for consistent deals

        if (consistencyReport != null){
            return false;
        }

        return true;
    }

    protected List<BasicDeal> getDealsToSupportMoveAndHoldOrders(List<Order> planOrders) {
        List<BasicDeal> dealsToOffer = new ArrayList<>();
        // use a mapping from every ally to its controlled regions
        Map<String, List<Region>> alliesRegions = this.getAlliesControlledRegions();

        // get order from plan and check which type of orders are advised by tactics module
        for (Order order : planOrders) {

            if (order instanceof HLDOrder) {
                HLDOrder holdOrder = (HLDOrder) order;
                this.getLogger().logln("" + this.botName + ": D-Brain advices Hold Order: " + holdOrder.getLocation(), true);
                List<BasicDeal> dealsToAdd = this.addDealsSupportHoldOrders(holdOrder, alliesRegions);
                dealsToOffer.addAll(dealsToAdd);
            }
//
//            else if (order instanceof MTOOrder){
//                MTOOrder moveOrder = (MTOOrder) order;
//                List<BasicDeal> dealsToAdd = this.addDealsSupportMoveOrders(moveOrder, alliesRegions);
//                dealsToOffer.addAll(dealsToAdd);
//            }
        }

        return dealsToOffer;
    }

    protected List<BasicDeal> addDealsSupportHoldOrders(HLDOrder holdOrder, Map<String, List<Region>> alliesRegions){
        List<BasicDeal> dealsToAdd = new ArrayList<>();
        Region holdUnit =  holdOrder.getLocation();
        List<Region> adjacentRegions = holdUnit.getAdjacentRegions();

        // iterate over adjacent regions
        // try to find a region controlled by one of our allies
        // offer him to support our hold order

        for (Region adjacentRegion: adjacentRegions) {
            for (String allyName : alliesRegions.keySet()) {
                if (alliesRegions.get(allyName).contains(adjacentRegion)) {

                    this.getLogger().logln("" + this.botName + ": Support Hold Order: Found an ally " + allyName + " with an adjacent unit " + adjacentRegion.getName()
                            + " offering mutual hold deal", true);

                    // create order commitment
                    Power allyPower = this.game.getPower(allyName);
                    Order supportMyHoldOrder = new SUPOrder(allyPower, adjacentRegion, holdOrder);
                    OrderCommitment allySupportOrderCommitment = new OrderCommitment(game.getYear(), game.getPhase(), supportMyHoldOrder);

                    // deals cannot contain only one power so add a commitment to support an hold of ally power in current region
                    HLDOrder allyHoldOrder = new HLDOrder(allyPower, adjacentRegion);
                    Order supportAllyHoldOrder = new SUPOrder(this.me, holdUnit, allyHoldOrder);
                    OrderCommitment mySupportOrderCommitment = new OrderCommitment(game.getYear(), game.getPhase(), supportAllyHoldOrder);

                    // create a deal
                    List<OrderCommitment> Commitments = new ArrayList<>();
                    Commitments.add(allySupportOrderCommitment);
                    Commitments.add(mySupportOrderCommitment);

                    List<DMZ> emptyDmzList = new ArrayList<>();
                    BasicDeal deal = new BasicDeal(Commitments, emptyDmzList);

                    // add deal to list of support hold deals
                    dealsToAdd.add(deal);
                }
            }
        }
        return dealsToAdd;
    }

    protected List<BasicDeal> addDealsSupportMoveOrders(MTOOrder moveOrder, Map<String, List<Region>> alliesRegions){
        List<BasicDeal> dealsToAdd = new ArrayList<>();
        Region sourceRegion = moveOrder.getLocation();
        Region destinationRegion = moveOrder.getDestination();

        // get adjacent regions to destination region
        List<Region> adjacentRegions = destinationRegion.getAdjacentRegions();

        // iterate over adjacent regions
        // try to find a region controlled by one of our allies
        // offer him to support our move order

        for (Region adjacentRegion: adjacentRegions) {
            for (String allyName : alliesRegions.keySet()) {
                if (alliesRegions.get(allyName).contains(adjacentRegion)) {

                    // create order commitment
                    Power allyPower = this.game.getPower(allyName);
                    Order supportMoveOrder = new SUPMTOOrder(allyPower, adjacentRegion, moveOrder);
                    OrderCommitment orderCommitment = new OrderCommitment(game.getYear(), game.getPhase(), supportMoveOrder);

                    // create a deal
                    List<OrderCommitment> singleCommitment = new ArrayList<>();
                    singleCommitment.add(orderCommitment);
                    List<DMZ> emptyDmzList = new ArrayList<>();
                    BasicDeal deal = new BasicDeal(singleCommitment, emptyDmzList);

                    // add deal to list of support hold deals
                    dealsToAdd.add(deal);
                }
            }
        }

        return dealsToAdd;
    }

    protected Map<String, List<Region>> getAlliesControlledRegions(){
        Map<String, List<Region>> alliesControlledRegions = new HashMap<>();
        for (Power ally: this.myNaturalAllies){
            String allyName = ally.getName();
            List<Region> allyRegions = ally.getControlledRegions();
            alliesControlledRegions.put(allyName, allyRegions);
        }
        return alliesControlledRegions;
    }

    protected Map<String, List<Region>> getPowersControlledRegions(){
        Map<String, List<Region>> powersControlledRegions = new HashMap<>();
        for (Power ally: this.game.getPowers()){
            String allyName = ally.getName();
            List<Region> allyRegions = ally.getControlledRegions();
            powersControlledRegions.put(allyName, allyRegions);
        }
        return powersControlledRegions;
    }
}
