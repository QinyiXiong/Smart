package com.sdumagicode.backend.service;

import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.core.service.Service;
import com.sdumagicode.backend.entity.Sponsor;

/**
 * @author ronger
 */
public interface SponsorService extends Service<Sponsor> {
    /**
     * 赞赏
     *
     * @param sponsor
     * @return
     * @throws ServiceException
     */
    boolean sponsorship(Sponsor sponsor) throws ServiceException;
}
