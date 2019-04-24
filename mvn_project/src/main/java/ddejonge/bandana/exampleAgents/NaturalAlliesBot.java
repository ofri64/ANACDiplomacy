package ddejonge.bandana.exampleAgents;

import java.util.ArrayList;
import java.util.List;

import ddejonge.bandana.anac.ANACNegotiator;
import ddejonge.bandana.negoProtocol.*;
import ddejonge.bandana.tools.Utilities;
import ddejonge.negoServer.Message;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.orders.Order;


public class NaturalAlliesBot extends ANACNegotiator {
    /**
     * Main method to start the agent
     *
     * @param args
     */

    public static void main(String[] args) {

        NaturalAlliesBot myPlayer = new NaturalAlliesBot(args);
        myPlayer.run();

    }

    private String botName;
    private boolean isFirstPeaceRound = true;
    private List<Power> coalitionMembers = new ArrayList<>();
    private boolean peaceToAllMode = true;
    private static int peaceSupplyCenterBoundThreshold = 7;

    //Constructor

    /**
     * Inheriting from ANACNegotiator calling it's constructor
     * and also initiating D-Brane's Tactics module
     *
     * @param args
     */
    public NaturalAlliesBot(String[] args) {
        super(args);
        botName = "NaturalAlliesBot";

    }

    /**
     * This method is automatically called at the start of the game, after the 'game' field is set.
     * <p>
     * It is called when the first NOW message is received from the game server.
     * The NOW message contains the current phase and the positions of all the units.
     * <p>
     * You are allowed, but not required, to implement this method
     */
    @Override
    public void start() {

        //You can use the logger to write stuff to the log file.
        //The location of the log file can be set through the command line option -log.
        //it is not necessary to call getLogger().enable() because this is already automatically done by the ANACNegotiator class.

        this.getLogger().logln("game is starting! let's go " + this.botName + "! good luck!", true);
        String myPowerName = this.me.getName();
        this.getLogger().logln("" + this.botName + " is playing as " + myPowerName, true);
    }

    /**
     * Each round, after each power has submitted its orders, this method is called several times:
     * once for each order submitted by any other power.
     */
    @Override
    public void receivedOrder(Order arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * This is where the magic happens!
     * Implementing the negotiations protocol!
     *
     * @param negotiationDeadline - maximum time allowed for negotiation procedure
     */


    @Override
    public void negotiate(long negotiationDeadline) {
        boolean startOfThisNegotiation = true;
        int mySupplyCenterNumber = this.me.getOwnedSCs().size();

        if (mySupplyCenterNumber >= peaceSupplyCenterBoundThreshold && peaceToAllMode){
            this.getLogger().logln(botName + ":Number of SC for " + me.getName() + " is now " + mySupplyCenterNumber + ". Changing to Back-stub mode", true);
            peaceToAllMode = false;
        }

        if (mySupplyCenterNumber < peaceSupplyCenterBoundThreshold && !peaceToAllMode){
            this.getLogger().logln(botName + ":Number of SC for " + me.getName() + " is now " + mySupplyCenterNumber + ". Changing to peace for all mode", true);
            peaceToAllMode = true;
            isFirstPeaceRound = true;
            coalitionMembers = new ArrayList<>();
        }

        //This loop repeats 2 steps. The first step is to handle any incoming messages,
        // while the second step tries to find deals to propose to the other negotiators.
        while (System.currentTimeMillis() < negotiationDeadline) {


            //STEP 1: Handle incoming messages.


            while (hasMessage()) {
                Message receivedMessage = removeMessageFromQueue();
                this.getLogger().logln("got message " + receivedMessage.getContent(), false);

                if (receivedMessage.getPerformative().equals(DiplomacyNegoClient.ACCEPT)) {
                    DiplomacyProposal acceptedProposal = (DiplomacyProposal) receivedMessage.getContent();
                    this.getLogger().logln("" + botName + ".negotiate() Received acceptance from " + receivedMessage.getSender() + ": " + acceptedProposal, false);

                    // This is to make sure this happens only in the first time.
                    if (isFirstPeaceRound && peaceToAllMode) {
                        List<String> dealParticipants = new ArrayList<>();
                        dealParticipants.add(receivedMessage.getSender());
                        for (String powerName : dealParticipants) {
                            addToCoalition(this.game.getPower(powerName));
                        }
                    }

                } else if (receivedMessage.getPerformative().equals(DiplomacyNegoClient.PROPOSE)) {
                    if (peaceToAllMode){ // only accept proposals in peach to all mode
                        DiplomacyProposal receivedProposal = (DiplomacyProposal) receivedMessage.getContent();
                        BasicDeal deal = (BasicDeal) receivedProposal.getProposedDeal();

                        if (checkProposedDealIsConsistentAndNotOutDated(deal)) {
                            this.acceptProposal(receivedProposal.getId());
                        }
                    }

                } else if (receivedMessage.getPerformative().equals(DiplomacyNegoClient.CONFIRM)) {
                    DiplomacyProposal confirmedProposal = (DiplomacyProposal) receivedMessage.getContent();
                    this.getLogger().logln("" + botName + ".negotiate() Received confirmed from " + receivedMessage.getSender() + ": " + confirmedProposal, false);


                } else if (receivedMessage.getPerformative().equals(DiplomacyNegoClient.REJECT)) {
                    DiplomacyProposal rejectedProposal = (DiplomacyProposal) receivedMessage.getContent();
                    this.getLogger().logln("" + botName + ".negotiate() Received reject from " + receivedMessage.getSender() + ": " + rejectedProposal, false);

                }

            }

            //STEP 2: offer deals.
            // only offer deals in peace to all mode
            if (startOfThisNegotiation && peaceToAllMode) {
                this.getLogger().logln(botName + ": " + me.getName() + " now offering deals.", true);

                List<BasicDeal> dealsToOffer = getDealsToOffer();
                for (BasicDeal deal : dealsToOffer) {
                    this.proposeDeal(deal);
                }
            }

            startOfThisNegotiation = false;


            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {;}

        }

        isFirstPeaceRound = false;
    }

    private List<BasicDeal> getDealsToOffer() {
        List<BasicDeal> dealsToOffer;

        if (isFirstPeaceRound) {
            List<Power> allPowers = game.getPowers();
            dealsToOffer = getDmzDealsSingleAlly(allPowers);

        } else {

            List<Power> aliveAllies = getAliveCoalitionMembers();

            if (aliveAllies.size() == 1) {
                dealsToOffer = getDmzDealsSingleAlly(aliveAllies);
            } else {
                dealsToOffer = getDmzDealsMultipleAllies(aliveAllies);
            }
        }

        return dealsToOffer;
    }

    private List<BasicDeal> getDmzDealsSingleAlly(List<Power> alliesPowers) {
        List<BasicDeal> dealsToOffer = new ArrayList<>();

        // offer mutual DMZ deals for each ally
        for (Power power : alliesPowers) {
            List<DMZ> demilitarizedZones = new ArrayList<>();
            if (power != this.me) {

                List<Power> currentAllyList = new ArrayList<>();
                currentAllyList.add(power);
                demilitarizedZones.add(new DMZ(game.getYear(), game.getPhase(), currentAllyList, me.getOwnedSCs()));

                List<Power> onlyMeAllyList = new ArrayList<>();
                onlyMeAllyList.add(me);
                demilitarizedZones.add(new DMZ(game.getYear(), game.getPhase(), onlyMeAllyList, power.getOwnedSCs()));

                List<OrderCommitment> emptyOrderCommitments = new ArrayList<>();
                BasicDeal deal = new BasicDeal(emptyOrderCommitments, demilitarizedZones);
                dealsToOffer.add(deal);
            }

        }
        return dealsToOffer;
    }

    private List<BasicDeal> getDmzDealsMultipleAllies(List<Power> aliveAllies) {
        List<BasicDeal> dealsToOffer = new ArrayList<>();

        // make offers for all the coalition members to not attack each other
        for (Power ally : aliveAllies) {
            List<DMZ> demilitarizedZones = new ArrayList<>();
            List<Power> otherCoalitionMembers = new ArrayList<>();
            otherCoalitionMembers.add(me);

            for (Power coalitionMember : aliveAllies) {
                if (coalitionMember != ally) {
                    otherCoalitionMembers.add(coalitionMember);
                }
            }

            demilitarizedZones.add(new DMZ(game.getYear(), game.getPhase(), otherCoalitionMembers, ally.getOwnedSCs()));
            List<OrderCommitment> emptyOrderCommitments = new ArrayList<>();
            BasicDeal deal = new BasicDeal(emptyOrderCommitments, demilitarizedZones);
            dealsToOffer.add(deal);
        }

        return dealsToOffer;
    }

    private void addToCoalition(Power power) {
        if (!coalitionMembers.contains(power)) {
            coalitionMembers.add(power);
        }
    }

    // This function returns all the alive coalition members.
    private List<Power> getAliveCoalitionMembers() {
        List<Power> allAliveNegotiatingPowers = this.getNegotiatingPowers();
        List<Power> aliveCoalitionMembers = new ArrayList<>();

        for (Power ally : coalitionMembers) {
            if (allAliveNegotiatingPowers.contains(ally) && !ally.equals(me)) {
                aliveCoalitionMembers.add(ally);
            }
        }

        return aliveCoalitionMembers;
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
        for (OrderCommitment orderCommitment : proposedDeal.getOrderCommitments()) {

            // Sometimes we may receive messages too late, so we check if the proposal does not
            // refer to some round of the game that has already passed.
            // one offer is enough to eliminate the entire deal
            if (isHistory(orderCommitment.getPhase(), orderCommitment.getYear())) {
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

        return consistencyReport == null;
    }
}