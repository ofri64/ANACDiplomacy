package ddejonge.bandana.exampleAgents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import ddejonge.bandana.anac.ANACNegotiator;
import ddejonge.bandana.negoProtocol.*;
import ddejonge.bandana.tools.Utilities;
import ddejonge.negoServer.Message;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.orders.*;


public class BackStabDiplomat extends ANACNegotiator {
    /**
     * Main method to start the agent
     *
     * @param args
     */

    public static void main(String[] args) {

        BackStabDiplomat myPlayer = new BackStabDiplomat(args);
        myPlayer.run();

    }

    private String botName;
    private boolean totalBackStabMode = false;
    private boolean backStabStrategyMode = false;
    private boolean isFirstRound = true;
    private int totalBackStabBeginYear = 1910;
    private int strategyBackStabBeginYear = 1907;
    private int backStabLowerBoundThreshold = 8;
    private List<Power> coalitionMembers = new ArrayList<>();
    private List<Power> coalitionCandidates = new ArrayList<>();
    private Random random = new Random();

    //Constructor

    /**
     * Inheriting from ANACNegotiator calling it's constructor
     * and also initiating D-Brane's Tactics module
     *
     * @param args
     */
    public BackStabDiplomat(String[] args) {
        super(args);
        botName = "BackStabDiplomat";

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

        // update coalition members and new candidates
        if (!this.isFirstRound) {
            this.removeWeakCoalitionMembers();
            List<Power> potentialCandidates = this.getNegotiatingPowers();
            this.coalitionCandidates = this.getCoalitionNewCandidates(potentialCandidates);
        }
        this.getLogger().logln(botName + ": My current coalition members are: " + Arrays.toString(coalitionMembers.toArray()), true);
        this.getLogger().logln(botName + ": My current coalition candidates are: " + Arrays.toString(coalitionCandidates.toArray()), true);

        // check for total back stab mode
        int mySupplyCenterNumber = this.me.getOwnedSCs().size();
        int currentYear = game.getYear();

        if (currentYear >= totalBackStabBeginYear && mySupplyCenterNumber >= backStabLowerBoundThreshold) {
            this.getLogger().logln(botName + ":Number of SC is now " + mySupplyCenterNumber + ". Changing to total Back-stab mode for current turn", true);
            totalBackStabMode = true;
        }

        // check for strategy back stab mode
        int maxNumScOutsideCoalition = getNumSupplyCentersForStrongestPowerOutsideCoalition();
        if (currentYear >= strategyBackStabBeginYear && maxNumScOutsideCoalition < 9){
            this.getLogger().logln(botName + ":Number of SC is now " + mySupplyCenterNumber + ". Changing to strategy Back-stab mode for current turn", true);
            backStabStrategyMode = true;
        }

        while (System.currentTimeMillis() < negotiationDeadline) {

            if (startOfThisNegotiation) {

                if (!totalBackStabMode) {
                    this.getLogger().logln(botName + ": " + me.getName() + " now offering deals.", true);

                    List<BasicDeal> dealsToOffer = getDealsToOffer();
                    for (BasicDeal deal : dealsToOffer) {
                        this.proposeDeal(deal);
                    }
                }
            }

            startOfThisNegotiation = false;

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                ;
            }

            while (hasMessage()) {
                Message receivedMessage = removeMessageFromQueue();

                if (receivedMessage.getPerformative().equals(DiplomacyNegoClient.ACCEPT)) {
                    DiplomacyProposal acceptedProposal = (DiplomacyProposal) receivedMessage.getContent();
                    this.getLogger().logln("" + botName + ".negotiate() Received acceptance from " + receivedMessage.getSender() + ": " + acceptedProposal, true);


                } else if (receivedMessage.getPerformative().equals(DiplomacyNegoClient.PROPOSE)) {
                    if (!totalBackStabMode) {
                        DiplomacyProposal receivedProposal = (DiplomacyProposal) receivedMessage.getContent();
                        BasicDeal deal = (BasicDeal) receivedProposal.getProposedDeal();

                        if (checkProposedDealIsConsistentAndNotOutDated(deal)) {
                            if (!isFirstRound) {
                                if (backStabStrategyMode) {
                                    if (this.acceptProposedDeal(receivedProposal)) {
                                        this.acceptProposal(receivedProposal.getId());

                                    } else {

                                        this.getLogger().logln("" + botName + ".negotiate() Denied the proposed deal " + receivedMessage.getSender() + ": " + receivedProposal, true);
                                    }
                                }

                                else{ // keep accepting proposals
                                    this.acceptProposal(receivedProposal.getId());
                                }
                            }


                            else { // first round - just accept
                                this.acceptProposal(receivedProposal.getId());
                            }
                        }
                    }

                } else if (receivedMessage.getPerformative().equals(DiplomacyNegoClient.CONFIRM)) {
                    DiplomacyProposal confirmedProposal = (DiplomacyProposal) receivedMessage.getContent();
                    this.getLogger().logln("" + botName + ".negotiate() Received confirmed from " + receivedMessage.getSender() + ": " + confirmedProposal, false);

                    List<String> dealParticipants = confirmedProposal.getParticipants();
                    for (String powerName : dealParticipants) {
                        addToCoalition(this.game.getPower(powerName));
                    }

                } else if (receivedMessage.getPerformative().equals(DiplomacyNegoClient.REJECT)) {
                    DiplomacyProposal rejectedProposal = (DiplomacyProposal) receivedMessage.getContent();
                    this.getLogger().logln("" + botName + ".negotiate() Received reject from " + receivedMessage.getSender() + ": " + rejectedProposal, false);

                }

            }
        }
        isFirstRound = false;
        totalBackStabMode = false;
    }

    private boolean acceptProposedDeal(DiplomacyProposal proposal) {
        double acceptanceThreshold = 1.7;

        List<String> participants = proposal.getParticipants();
        List<Power> participantsPowers = this.getListOfPowers(participants);

        for (Power participantPower : participantsPowers) {
            if (!this.isCoalitionMember(participantPower)) {
                if (!this.isCoalitionCandidate(participantPower)) {
                    acceptanceThreshold -= 0.3;
                } else {
                    acceptanceThreshold -= 0.2;
                }
            }
        }

        double participantsStrength = this.getParticipantsAverageStrength(participantsPowers);
        double myPower = me.getOwnedSCs().size();

        if (myPower > participantsStrength) {
            acceptanceThreshold -= 0.4;
        } else {

            if (participantsStrength <= 5) {
                acceptanceThreshold -= 0.25;

            } else if (participantsStrength <= 4) {
                acceptanceThreshold -= 0.2;
            } else if (participantsStrength <= 3) {
                acceptanceThreshold -= 0.4;
            }
        }

        double acceptanceProbability = this.random.nextDouble();
        return acceptanceProbability <= acceptanceThreshold; // accept a deal if probability below threshold
    }

    private List<BasicDeal> getDealsToOffer() {
        List<BasicDeal> dealsToOffer = new ArrayList<>();

        if (isFirstRound) {
            List<Power> allPowers = game.getPowers();
            List<BasicDeal> dmzDealsSingleAlly = getDmzDealsSingleAlly(allPowers);
            dealsToOffer.addAll(dmzDealsSingleAlly);

        } else {

            List<Power> aliveAllies = getAliveCoalitionMembers();

            if (aliveAllies.size() == 1) {
                List<BasicDeal> dmzDealsSingleAlly = getDmzDealsSingleAlly(aliveAllies);
                dealsToOffer.addAll(dmzDealsSingleAlly);

            } else {

                List<BasicDeal> dmzDealsMultipleAllies = getDmzDealsMultipleAllies(aliveAllies);
                dealsToOffer.addAll(dmzDealsMultipleAllies);
            }

            List<BasicDeal> dmzDealsForCandidates = getDmzDealsSingleAlly(this.coalitionCandidates);
            dealsToOffer.addAll(dmzDealsForCandidates);
        }

        return dealsToOffer;
    }

    private List<BasicDeal> getDmzDealsSingleAlly(List<Power> alliesPowers) {
        List<BasicDeal> dealsToOffer = new ArrayList<>();

        // offer mutual DMZ deals for each ally
        for (Power power : alliesPowers) {
            if (power != this.me) {
                List<DMZ> demilitarizedZones = new ArrayList<>();

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

            List<Power> otherCoalitionMembers = new ArrayList<>();
            otherCoalitionMembers.add(me);

            for (Power coalitionMember : aliveAllies) {
                if (coalitionMember != ally) {
                    otherCoalitionMembers.add(coalitionMember);

                    List<DMZ> demilitarizedZones = new ArrayList<>();
                    demilitarizedZones.add(new DMZ(game.getYear(), game.getPhase(), otherCoalitionMembers, ally.getOwnedSCs()));
                    List<OrderCommitment> emptyOrderCommitments = new ArrayList<>();
                    BasicDeal deal = new BasicDeal(emptyOrderCommitments, demilitarizedZones);
                    dealsToOffer.add(deal);
                }
            }

        }

        return dealsToOffer;
    }

    private void addToCoalition(Power power) {
        if (!coalitionMembers.contains(power)) {
            this.getLogger().logln(botName + ": Adding to coalition " + power.getName(), true);
            coalitionMembers.add(power);
        }
    }


    private boolean isCoalitionMember(Power power) {
        return this.coalitionMembers.contains(power);
    }

    private boolean isCoalitionCandidate(Power power) {
        return this.coalitionCandidates.contains(power);
    }

    private List<Power> getListOfPowers(List<String> powerNames) {
        List<Power> powers = new ArrayList<>();
        for (String powerName : powerNames) {
            powers.add(this.game.getPower(powerName));
        }

        return powers;
    }

    private double getParticipantsAverageStrength(List<Power> participants) {
        double totalNumSupplyCenters = 0;

        for (Power power : participants) {
            totalNumSupplyCenters += power.getOwnedSCs().size();
        }

        return totalNumSupplyCenters / participants.size();
    }

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

    private void removeWeakCoalitionMembers() {
        List<Power> aliveCoalitionMembers = this.getAliveCoalitionMembers();
        List<Power> nextCoalitionMembers = new ArrayList<>();

        for (Power coalitionMember : aliveCoalitionMembers) {
            double acceptanceThreshold = 0;
            int memberNumCurrentSupplyCenters = coalitionMember.getOwnedSCs().size();

            switch (memberNumCurrentSupplyCenters) {
                case 1:
                    acceptanceThreshold += 0.8;
                    break;
                case 2:
                    acceptanceThreshold += 0.5;
                    break;
                case 3:
                    acceptanceThreshold += 0.3;
                    break;
                case 4:
                    acceptanceThreshold += 0.2;
                    break;
                case 5:
                    acceptanceThreshold += 0.05;
                    break;
            }

            double randomProbability = this.random.nextDouble();
            if (randomProbability <= acceptanceThreshold) {
                // instead of removing and causing ConcurrentModificationException -
                // just don't add to new list
                this.getLogger().logln(botName + ": Removing from coalition " + coalitionMember.getName(), true);
            }
            else{
                nextCoalitionMembers.add(coalitionMember);
            }
        }

        this.coalitionMembers = nextCoalitionMembers;
    }

    private List<Power> getCoalitionNewCandidates(List<Power> candidatePowers) {
        List<Power> coalitionNewCandidates = new ArrayList<>();

        for (Power candidate : candidatePowers) {

            // skip on current coalition members
            if (this.isCoalitionMember(candidate) || candidate == me) {
                continue;
            }

            double acceptanceThreshold = 0;
            int myNumOfSupplyCenters = me.getOwnedSCs().size();
            int candidateNumSupplyCenters = candidate.getOwnedSCs().size();

            if (candidateNumSupplyCenters > myNumOfSupplyCenters) {
                acceptanceThreshold += 0.8;
            } else {

                if (candidateNumSupplyCenters >= 8) {
                    acceptanceThreshold += 0.8;
                } else if (candidateNumSupplyCenters >= 5) {
                    acceptanceThreshold += 0.1 * candidateNumSupplyCenters;
                } else if (candidateNumSupplyCenters >= 3) {
                    acceptanceThreshold += 0.25;
                } else {
                    acceptanceThreshold += 0.1;
                }
            }

            // add to new candidates list
            double acceptProbability = this.random.nextDouble();
            if (acceptProbability <= acceptanceThreshold) {
                this.getLogger().logln(botName + ": Adding to new candidates list " + candidate.getName(), true);
                coalitionNewCandidates.add(candidate);
            }

        }

        return coalitionNewCandidates;
    }

    private boolean checkProposedDealIsConsistentAndNotOutDated(BasicDeal proposedDeal) {
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

    private int getNumSupplyCentersForStrongestPowerOutsideCoalition(){
        int maxNumSc = 0;
        List<Power> allPowers = this.game.getPowers();
        for (Power power : allPowers){
            if (!this.isCoalitionMember(power)){
                int powerNumSc = power.getOwnedSCs().size();
                if (powerNumSc > maxNumSc){
                    maxNumSc = powerNumSc;
                }
            }
        }

        return maxNumSc;
    }

}