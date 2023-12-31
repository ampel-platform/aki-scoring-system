package de.uni_leipzig.mds.feed;

public final class DTOUtil {

    public static PatientCaseFeed domainFromDto(PatientCaseFeedDTO dto) {
        return new PatientCaseFeed(dto);
    }

    public static PatientCaseFeedDTO dtoFromDomain(PatientCaseFeed feed) {
        return new PatientCaseFeedDTO(feed);
    }
}
