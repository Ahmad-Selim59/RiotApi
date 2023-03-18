public List<MatchParticipant> findUniqueGameParticipantsFromPlayerUUID(String rootPlayerPUUID, int maxPlayers, List<String> matchesToIgnore, int matchesPerPlayer) {
    HashSet<MatchParticipant> uniqueParticipants = new HashSet<>();
    LinkedList<MatchParticipant> frontier = new LinkedList<>();
    
    List<String> matchesToProcess = new ArrayList<>();
    HashSet<String> processedMatches = new HashSet<>();

    String currentPUUID = rootPlayerPUUID;

    while (uniqueParticipants.size() < maxPlayers) {
        String[] fetchedMatches = null;
        try {
            fetchedMatches = loser
                .getRiotAPI()
                .getMatchManager()
                .getSummonerMatchIds(currentPUUID, "?queue=420&type=ranked&start=0&count=" + matchesPerPlayer);
        } catch (Exception e) {
            // Handle API call limit or any other exception here
            System.err.println("Error fetching matches: " + e.getMessage());
            break;
        }

        if (fetchedMatches == null || fetchedMatches.length == 0) {
            break; // No matches found or the root player game history is empty
        }

        for (String match : fetchedMatches) {
            if (!(matchesToIgnore.contains(match) || processedMatches.contains(match))) matchesToProcess.add(match); // don't crawl matches twice
        }

        Iterator<String> matchIter = matchesToProcess.iterator();
        while (uniqueParticipants.size() < maxPlayers && matchIter.hasNext()) {
            String match = matchIter.next();
            MatchDto matchDto = null;
            try {
                matchDto = loser.getRiotAPI().getMatchManager().getMatchById(match);
            } catch (Exception e) {
                // Handle API call limit or any other exception here
                System.err.println("Error fetching match details: " + e.getMessage());
                continue;
            }
            
            if (matchDto == null) {
                continue;
            }

            MatchParticipant[] matchParticipants = matchDto.getMatchInfo().getParticipants();
            for (int i = 0; uniqueParticipants.size() < maxPlayers && i < matchParticipants.length; i++) {
                if (!uniqueParticipants.contains(matchParticipants[i])) {
                    uniqueParticipants.add(matchParticipants[i]);
                    frontier.add(matchParticipants[i]); // this crawles the whole tree - you could limit this by clamping the size of the list
                }
            }
            processedMatches.add(match);
        }
        matchesToProcess.clear();
        currentPUUID = frontier.poll();
        if (currentPUUID == null) {
            break; // we crawled everything
        }

    }

    return new ArrayList<>(uniqueParticipants);
}