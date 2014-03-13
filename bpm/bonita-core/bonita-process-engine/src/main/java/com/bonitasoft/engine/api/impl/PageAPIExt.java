<<<<<<< HEAD
package com.bonitasoft.engine.api.impl;

import java.util.List;

import org.bonitasoft.engine.api.impl.SessionInfos;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;

import com.bonitasoft.engine.api.PageAPI;
import com.bonitasoft.engine.api.impl.transaction.page.AddPage;
import com.bonitasoft.engine.api.impl.transaction.page.SearchPages;
import com.bonitasoft.engine.api.impl.transaction.reporting.GetReport;
import com.bonitasoft.engine.page.Page;
import com.bonitasoft.engine.page.PageCreator;
import com.bonitasoft.engine.page.PageNotFoundException;
import com.bonitasoft.engine.page.PageService;
import com.bonitasoft.engine.page.SPage;
import com.bonitasoft.engine.search.descriptor.SearchEntitiesDescriptor;
import com.bonitasoft.engine.service.SPModelConvertor;
import com.bonitasoft.engine.service.TenantServiceAccessor;
import com.bonitasoft.engine.service.impl.ServiceAccessorFactory;
import com.bonitasoft.engine.service.impl.TenantServiceSingleton;

public class PageAPIExt implements PageAPI {

    @Override
    public Page getPage(final long pageId) throws PageNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] getPageContent(final long pageId) throws PageNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SearchResult<Page> searchPages(final SearchOptions searchOptions) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final PageService pageService = tenantAccessor.getPageService();
        final SearchPages searchPages = new SearchPages(pageService, searchEntitiesDescriptor.getSearchPageDescriptor(), searchOptions);
        try {
            searchPages.execute();
            return searchPages.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    @Override
    public Page createPage(final PageCreator pageCreator, final byte[] content) throws AlreadyExistsException, CreationException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final long userId = SessionInfos.getUserIdFromSession();
        PageService pageService = tenantAccessor.getPageService();
        final SPage sPage = SPModelConvertor.constructSPage(pageCreator, userId);
        final AddPage addPage = new AddPage(pageService, sPage, content);
        checkPageAlreadyExists((String) pageCreator.getFields().get(PageCreator.PageField.NAME), tenantAccessor);
        try {
            addPage.execute();
            return SPModelConvertor.toPage(addPage.getResult());
        } catch (final SBonitaException sbe) {
            throw new CreationException(sbe);
        }
    }

    @Override
    public void deletePage(final long pageId) throws DeletionException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deletePages(final List<Long> pageIds) throws DeletionException {
        // TODO Auto-generated method stub

    }

    private static TenantServiceAccessor getTenantAccessor() {
        try {
            final SessionAccessor sessionAccessor = ServiceAccessorFactory.getInstance().createSessionAccessor();
            final long tenantId = sessionAccessor.getTenantId();
            return TenantServiceSingleton.getInstance(tenantId);
        } catch (final Exception e) {
            throw new BonitaRuntimeException(e);
        }
    }

    protected void checkPageAlreadyExists(final String name, final TenantServiceAccessor tenantAccessor) throws AlreadyExistsException {
        // Check if the problem is primary key duplication:
        try {
            final GetReport getReport = new GetReport(tenantAccessor, name);

            getReport.execute();
            if (getReport.getResult() != null) {
                throw new AlreadyExistsException("A report already exists with the name " + name);
            }
        } catch (SBonitaException e) {
            // ignore it
        }
    }

}
=======
package com.bonitasoft.engine.api.impl;

import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.engine.api.impl.SessionInfos;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.exceptions.SObjectNotFoundException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.search.impl.SearchResultImpl;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;

import com.bonitasoft.engine.api.PageAPI;
import com.bonitasoft.engine.page.Page;
import com.bonitasoft.engine.page.PageCreator;
import com.bonitasoft.engine.page.PageNotFoundException;
import com.bonitasoft.engine.page.PageService;
import com.bonitasoft.engine.page.SPage;
import com.bonitasoft.engine.service.SPModelConvertor;
import com.bonitasoft.engine.service.TenantServiceAccessor;
import com.bonitasoft.engine.service.impl.ServiceAccessorFactory;
import com.bonitasoft.engine.service.impl.TenantServiceSingleton;

public class PageAPIExt implements PageAPI {

    @Override
    public Page getPage(final long pageId) throws PageNotFoundException {
        final PageService pageService = getTenantAccessor().getPageService();

        try {
            SPage sPage = pageService.getPage(pageId);
            return convertToPage(sPage);
        } catch (SBonitaReadException e) {
            throw new PageNotFoundException(e);
        } catch (SObjectNotFoundException e) {
            throw new PageNotFoundException(e);
        }
    }

    @Override
    public byte[] getPageContent(final long pageId) throws PageNotFoundException {
        final PageService pageService = getTenantAccessor().getPageService();

        try {
            byte[] content = pageService.getPageContent(pageId);
            return content;
        } catch (SBonitaReadException e) {
            throw new PageNotFoundException(e);
        } catch (SObjectNotFoundException e) {
            throw new PageNotFoundException(e);
        }
    }

    @Override
    public SearchResult<Page> searchPages(final SearchOptions searchOptions) throws SearchException {
        return new SearchResultImpl<Page>(0, new ArrayList<Page>(0));
    }

    @Override
    public Page createPage(final PageCreator pageCreator, final byte[] content) throws AlreadyExistsException, CreationException {
        final PageService pageService = getTenantAccessor().getPageService();
        final long userId = getUserIdFromSessionInfos();
        final SPage sPage = constructPage(pageCreator, userId);

        checkPageAlreadyExists((String) pageCreator.getFields().get(PageCreator.PageField.NAME), getTenantAccessor());
        try {
            SPage addPage = pageService.addPage(sPage, content);
            return convertToPage(addPage);
        } catch (final SBonitaException sBonitaException) {
            throw new CreationException(sBonitaException);
        }
    }

    protected Page convertToPage(SPage addPage) {
        return SPModelConvertor.toPage(addPage);
    }

    protected SPage constructPage(final PageCreator pageCreator, final long userId) {
        final SPage sPage = SPModelConvertor.constructSPage(pageCreator, userId);
        return sPage;
    }

    protected long getUserIdFromSessionInfos() {
        final long userId = SessionInfos.getUserIdFromSession();
        return userId;
    }

    @Override
    public void deletePage(final long pageId) throws DeletionException {
        final PageService pageService = getTenantAccessor().getPageService();
        try {
            pageService.deletePage(pageId);

        } catch (final SBonitaException sBonitaException) {
            throw new DeletionException(sBonitaException);
        }
    }

    @Override
    public void deletePages(final List<Long> pageIds) throws DeletionException {
        final PageService pageService = getTenantAccessor().getPageService();
        try {
            for (Long pageId : pageIds) {
                pageService.deletePage(pageId);
            }
        } catch (final SBonitaException sBonitaException) {
            throw new DeletionException(sBonitaException);
        }

    }

    protected TenantServiceAccessor getTenantAccessor() {
        try {
            final SessionAccessor sessionAccessor = ServiceAccessorFactory.getInstance().createSessionAccessor();
            final long tenantId = sessionAccessor.getTenantId();
            return TenantServiceSingleton.getInstance(tenantId);
        } catch (final Exception e) {
            throw new BonitaRuntimeException(e);
        }
    }

    protected void checkPageAlreadyExists(final String name, final TenantServiceAccessor tenantAccessor) throws AlreadyExistsException {
        final PageService pageService = getTenantAccessor().getPageService();
        // Check if the problem is primary key duplication:
        try {
            final SPage sPage = pageService.getPageByName(name);
            if (sPage != null) {
                throw new AlreadyExistsException("A report already exists with the name " + name);
            }
        } catch (SBonitaException e) {
            // ignore it
        }
    }

}
>>>>>>> BS-7593, implements pageApi ext
