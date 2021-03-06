/*
 * Copyright (c) 2002-2015, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.appointment.web;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;

import fr.paris.lutece.plugins.appointment.business.AppointmentForm;
import fr.paris.lutece.plugins.appointment.business.AppointmentFormHome;
import fr.paris.lutece.plugins.appointment.business.AppointmentFormMessages;
import fr.paris.lutece.plugins.appointment.business.AppointmentFormMessagesHome;
import fr.paris.lutece.plugins.appointment.business.AppointmentHome;
import fr.paris.lutece.plugins.appointment.business.calendar.AppointmentDay;
import fr.paris.lutece.plugins.appointment.business.calendar.AppointmentDayHome;
import fr.paris.lutece.plugins.appointment.business.calendar.AppointmentHoliDaysHome;
import fr.paris.lutece.plugins.appointment.business.calendar.AppointmentSlot;
import fr.paris.lutece.plugins.appointment.business.calendar.AppointmentSlotHome;
import fr.paris.lutece.plugins.appointment.business.template.CalendarTemplateHome;
import fr.paris.lutece.plugins.appointment.service.AppointmentFormService;
import fr.paris.lutece.plugins.appointment.service.AppointmentResourceIdService;
import fr.paris.lutece.plugins.appointment.service.AppointmentService;
import fr.paris.lutece.plugins.appointment.service.AppointmentSlotService;
import fr.paris.lutece.plugins.appointment.service.EntryService;
import fr.paris.lutece.plugins.appointment.service.EntryTypeService;
import fr.paris.lutece.plugins.genericattributes.business.Entry;
import fr.paris.lutece.plugins.genericattributes.business.EntryFilter;
import fr.paris.lutece.plugins.genericattributes.business.EntryHome;
import fr.paris.lutece.plugins.genericattributes.business.Field;
import fr.paris.lutece.plugins.genericattributes.business.FieldHome;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.admin.AdminUserService;
import fr.paris.lutece.portal.service.captcha.CaptchaSecurityService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.image.ImageResource;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.rbac.RBACService;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.portal.service.workflow.WorkflowService;
import fr.paris.lutece.portal.util.mvc.admin.MVCAdminJspBean;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.utils.MVCUtils;
import fr.paris.lutece.portal.web.upload.MultipartHttpServletRequest;
import fr.paris.lutece.portal.web.util.LocalizedPaginator;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.date.DateUtil;
import fr.paris.lutece.util.html.Paginator;
import fr.paris.lutece.util.sql.TransactionManager;
import fr.paris.lutece.util.url.UrlItem;


/**
 * This class provides the user interface to manage AppointmentForm features (
 * manage, create, modify, remove )
 */
@Controller( controllerJsp = "ManageAppointmentForms.jsp", controllerPath = "jsp/admin/plugins/appointment/", right = AppointmentFormJspBean.RIGHT_MANAGEAPPOINTMENTFORM )
public class AppointmentFormJspBean extends MVCAdminJspBean
{
    /**
     * Right to manage appointment forms
     */
    public static final String RIGHT_MANAGEAPPOINTMENTFORM = "APPOINTMENT_FORM_MANAGEMENT";
    private static final long serialVersionUID = -615061018633136997L;

    // Constants
    private static final String EMPTY_STRING = "";

    // templates
    private static final String TEMPLATE_MANAGE_APPOINTMENTFORMS = "/admin/plugins/appointment/appointmentform/manage_appointmentforms.html";
    private static final String TEMPLATE_MANAGE_APPOINTMENTFORMSFIRSTSTEP = "/admin/plugins/appointment/appointmentform/manage_appointmentforms_gru_appointment.html";
    private static final String TEMPLATE_CREATE_APPOINTMENTFORM = "/admin/plugins/appointment/appointmentform/create_appointmentform.html";
    private static final String TEMPLATE_MODIFY_APPOINTMENTFORM = "/admin/plugins/appointment/appointmentform/modify_appointmentform.html";
    private static final String TEMPLATE_MODIFY_APPOINTMENTFORM_MESSAGES = "/admin/plugins/appointment/appointmentform/modify_appointmentform_messages.html";
    private static final String TEMPLATE_ADVANCED_MODIFY_APPOINTMENTFORM = "/admin/plugins/appointment/appointmentform/modify_advanced_appointmentform.html";
    private static final String TEMPLATE_MODIFY_APPOINTMENT_FORM = "/admin/plugins/appointment/appointmentform/modify_form_appointmentform.html";

    // Parameters
    private static final String PARAMETER_ID_FORM = "id_form";
    private static final String PARAMETER_ID_START = "date_start_validity";
    private static final String PARAMETER_ID_END = "date_end_validity";
    private static final String PARAMETER_BACK = "back";
    private static final String PARAMETER_PAGE_INDEX = "page_index";
    private static final String PARAMETER_FROM_DASHBOARD = "fromDashboard";
    private static final String PARAMETER_FORCE_RELOAD = "forceReload";
    private static final String PARAMETER_ICON_RESSOURCE = "image_resource";
    private static final String PARAMETER_DATE_MIN = "dateMin";
    private static final String PARAMETER_NAME_FORM = "formName";
    private static final String PARAMETER_DELETE_ICON = "deleteIcon";
    private static final String PARAMETER_FIRST_FORM = "first_form";
    private static final String PARAMETER_SECOND_FORM = "second_form";
    private static final String PARAMETER_FORM_RDV = "form_rdv";
    private static final String PARAMETER_GEOLOC_ADDRESS  = "geoloc_address";
    private static final String PARAMETER_GEOLOC_LATITUDE  = "geoloc_latitude";
    private static final String PARAMETER_GEOLOC_LONGITUDE  = "geoloc_longitude";

    private static final String PARAMETER_FIRSTNAME = "fn";
    private static final String PARAMETER_LASTNAME = "ln";
    private static final String PARAMETER_PHONE = "ph";
    private static final String PARAMETER_EMAILM = "em";
    private static final String PARAMETER_CUSTOMER_ID = "cid";
    private static final String PARAMETER_USER_ID_OPAM = "guid";
    

    // Properties for page titles
    private static final String PROPERTY_PAGE_TITLE_MANAGE_APPOINTMENTFORMS = "appointment.manage_appointmentforms.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_MODIFY_APPOINTMENTFORM = "appointment.modify_appointmentForm.titleAlterablesParameters";
    private static final String PROPERTY_PAGE_TITLE_MODIFY_APPOINTMENTFORM_TIME_OUT = "appointment.modify_appointmentForm.timeOutDuration";
    private static final String PROPERTY_PAGE_TITLE_MODIFY_APPOINTMENT_FORM = "appointment.modify_appointmentform.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_CREATE_APPOINTMENTFORM = "appointment.create_appointmentform.pageTitle";
    private static final String PROPERTY_PAGE_TITLE_MODIFY_APPOINTMENTFORM_MESSAGES = "appointment.modify_appointmentform_messages.pageTitle";

    // Markers
    private static final String MARK_APPOINTMENTFORM_LIST = "appointmentform_list";
    private static final String MARK_APPOINTMENTFORM = "appointmentform";
    private static final String MARK_PAGINATOR = "paginator";
    private static final String MARK_NB_ITEMS_PER_PAGE = "nb_items_per_page";
    private static final String MARK_ENTRY_LIST = "entry_list";
    private static final String MARK_ENTRY_TYPE_LIST = "entry_type_list";
    private static final String MARK_GROUP_ENTRY_LIST = "entry_group_list";
    private static final String MARK_LIST_ORDER_FIRST_LEVEL = "listOrderFirstLevel";
    private static final String MARK_LIST_WORKFLOWS = "listWorkflows";
    private static final String MARK_IS_CAPTCHA_ENABLED = "isCaptchaEnabled";
    private static final String MARK_FORM_MESSAGE = "formMessage";
    private static final String MARK_WEBAPP_URL = "webapp_url";
    private static final String MARK_LOCALE = "language";
    private static final String MARK_LOCALE_TINY = "locale";
    private static final String MARK_APPOINTMENT_RESOURCE_ENABLED = "isResourceInstalled";
    private static final String MARK_REF_LIST_CALENDAR_TEMPLATES = "refListCalendarTemplates";
    private static final String MARK_NULL = "NULL";
    private static final String MARK_PAGE = "page";
    private static final String MARK_FALSE = "false";

    // Jsp
    private static final String JSP_MANAGE_APPOINTMENTFORMS = "jsp/admin/plugins/appointment/ManageAppointmentForms.jsp";

    // Properties
    private static final String MESSAGE_CONFIRM_REMOVE_APPOINTMENTFORM = "appointment.message.confirmRemoveAppointmentForm";
    private static final String PROPERTY_DEFAULT_LIST_APPOINTMENTFORM_PER_PAGE = "appointment.listAppointmentForms.itemsPerPage";
    private static final String VALIDATION_ATTRIBUTES_PREFIX = "appointment.model.entity.appointmentform.attribute.";
    private static final String ERROR_MESSAGE_TIME_START_AFTER_TIME_END = "appointment.message.error.timeStartAfterTimeEnd";
    private static final String ERROR_MESSAGE_TIME_START_AFTER_DATE_END = "appointment.message.error.dateStartAfterTimeEnd";
    private static final String PROPERTY_MODULE_APPOINTMENT_RESOURCE_NAME = "appointment.moduleAppointmentResource.name";
    private static final String ERROR_MESSAGE_APPOINTMENT_FAILED = "appointment.message.error.formatDaysBeforeAppointment";
    private static final String ERROR_MESSAGE_APPOINTMENT_SUPERIOR = "appointment.message.error.formatDaysBeforeAppointmentSuperior";
    private static final String ERROR_MESSAGE_APPOINTMENT_SUPERIOR_MIDDLE = "appointment.message.error.formatDaysBeforeAppointmentMiddleSuperior";
    private static final String ERROR_MESSAGE_APPOINTMENT_PARSING_DATE = "appointment.message.error.dayDateFormat";
    private static final String MESSAGE_ERROR_DAY_DURATION_APPOINTMENT_NOT_MULTIPLE_FORM = "appointment.message.error.durationAppointmentDayNotMultipleForm";
    private static final String ERROR_MESSAGE_APPOINTMENT_DATES = "appointment.message.error.dateStartTimeEnd";
    private static final String MESSAGE_ERROR_MODIFY_FORM_HAS_APPOINTMENTS = "appointment.message.error.refreshDays.modifyFormHasAppointments";
    private static final String MESSAGE_ERROR_START_DATE_EMPTY = "appointment.message.error.startDateEmpty";
    private static final String MESSAGE_ERROR_DATE_LIMIT_NB_WEEK_EMPTY = "appointment.message.error.datelimit.nbWeek";
    private static final String PROPERTY_COPY_OF_FORM = "appointment.manage_appointmentforms.Copy";
    private static final String MESSAGE_ERROR_NUMBER_OF_SEATS_BOOKED = "appointment.message.error.numberOfSeatsBookedAndConcurrentAppointments";

    // Views
    private static final String VIEW_MANAGE_APPOINTMENTFORMS = "manageAppointmentForms";
    private static final String VIEW_MANAGE_APPOINTMENTFIRSTFORMS = "manageAppointmentFirstForms";
    private static final String VIEW_CREATE_APPOINTMENTFORM = "createAppointmentForm";
    private static final String VIEW_MODIFY_APPOINTMENTFORM = "modifyAppointmentForm";
    private static final String VIEW_ADVANCED_MODIFY_APPOINTMENTFORM = "modifyAppointmentFormAdvanced";
    private static final String VIEW_MODIFY_FORM_MESSAGES = "modifyAppointmentFormMessages";
    private static final String VIEW_PERMISSIONS_FORM = "permissions";

    // Actions
    private static final String ACTION_CREATE_APPOINTMENTFORM = "createAppointmentForm";
    private static final String ACTION_MODIFY_APPOINTMENTFORM = "modifyAppointmentForm";
    private static final String ACTION_REMOVE_APPOINTMENTFORM = "removeAppointmentForm";
    private static final String ACTION_CONFIRM_REMOVE_APPOINTMENTFORM = "confirmRemoveAppointmentForm";
    private static final String ACTION_DO_CHANGE_FORM_ACTIVATION = "doChangeFormActivation";
    private static final String ACTION_DO_MODIFY_FORM_MESSAGES = "doModifyAppointmentFormMessages";
    private static final String ACTION_DO_COPY_FORM = "doCopyAppointmentForm";

    // Infos
    private static final String INFO_APPOINTMENTFORM_CREATED = "appointment.info.appointmentform.created";
    private static final String INFO_APPOINTMENTFORM_UPDATED = "appointment.info.appointmentform.updated";
    private static final String INFO_APPOINTMENTFORM_REMOVED = "appointment.info.appointmentform.removed";
    private static final String INFO_MODIFY_APPOINTMENTFORM_SLOTS_UPDATED = "appointment.info.appointmentform.updated.slotsUpdated";

    // Session variable to store working values
    private static final String SESSION_ATTRIBUTE_APPOINTMENT_FORM = "appointment.session.appointmentForm";
    private static final String SESSION_CURRENT_PAGE_INDEX = "appointment.session.appointmentForm.currentPageIndex";
    private static final String SESSION_ITEMS_PER_PAGE = "appointment.session.appointmentForm.itemsPerPage";
    private static final String DEFAULT_CURRENT_PAGE = "1";

    // Local variables
    private static final CaptchaSecurityService _captchaSecurityService = new CaptchaSecurityService(  );
    private final EntryService _entryService = EntryService.getService(  );
    private final AppointmentFormService _appointmentFormService = SpringContextService.getBean( AppointmentFormService.BEAN_NAME );
    private int _nDefaultItemsPerPage;

    /**
     * Default constructor
     */
    public AppointmentFormJspBean(  )
    {
        _nDefaultItemsPerPage = AppPropertiesService.getPropertyInt( PROPERTY_DEFAULT_LIST_APPOINTMENTFORM_PER_PAGE, 50 );
    }

    /**
     * Get the page to manage appointment forms
     * @param request the request
     * @return The HTML content to display
     */
    @View( value = VIEW_MANAGE_APPOINTMENTFORMS, defaultView = true )
    public String getManageAppointmentForms( HttpServletRequest request )
    {
        String strCurrentPageIndex = Paginator.getPageIndex( request, Paginator.PARAMETER_PAGE_INDEX,
                (String) request.getSession(  ).getAttribute( SESSION_CURRENT_PAGE_INDEX ) );

        if ( strCurrentPageIndex == null )
        {
            strCurrentPageIndex = DEFAULT_CURRENT_PAGE;
        }

        request.getSession(  ).setAttribute( SESSION_CURRENT_PAGE_INDEX, strCurrentPageIndex );

        int nItemsPerPage = Paginator.getItemsPerPage( request, Paginator.PARAMETER_ITEMS_PER_PAGE,
                getIntSessionAttribute( request.getSession(  ), SESSION_ITEMS_PER_PAGE ), _nDefaultItemsPerPage );
        request.getSession(  ).setAttribute( SESSION_ITEMS_PER_PAGE, nItemsPerPage );

        request.getSession(  ).removeAttribute( SESSION_ATTRIBUTE_APPOINTMENT_FORM );

        UrlItem url = new UrlItem( JSP_MANAGE_APPOINTMENTFORMS );
        String strUrl = url.getUrl(  );
        List<AppointmentForm> listAppointmentForms = AppointmentFormHome.getAppointmentFormsList(  );

        // PAGINATOR
        LocalizedPaginator<AppointmentForm> paginator = new LocalizedPaginator<AppointmentForm>( listAppointmentForms,
                nItemsPerPage, strUrl, PARAMETER_PAGE_INDEX, strCurrentPageIndex, getLocale(  ) );

        Map<String, Object> model = getModel(  );

        model.put( MARK_NB_ITEMS_PER_PAGE, Integer.toString( nItemsPerPage ) );
        model.put( MARK_PAGINATOR, paginator );

        model.put( MARK_APPOINTMENTFORM_LIST,
            RBACService.getAuthorizedCollection( paginator.getPageItems(  ),
                AppointmentResourceIdService.PERMISSION_VIEW_FORM, AdminUserService.getAdminUser( request ) ) );

        model.put( VIEW_PERMISSIONS_FORM,
            getPermissions( paginator.getPageItems(  ), AdminUserService.getAdminUser( request ) ) );

        return getPage( PROPERTY_PAGE_TITLE_MANAGE_APPOINTMENTFORMS, TEMPLATE_MANAGE_APPOINTMENTFORMS, model );
    }

    /**
     * Get the page to manage appointment forms
     * @param request the request
     * @return The HTML content to display
     */
    @View( value = VIEW_MANAGE_APPOINTMENTFIRSTFORMS )
    public String getManageAppointmentFirstForms( HttpServletRequest request )
    {
        String strCurrentPageIndex = Paginator.getPageIndex( request, Paginator.PARAMETER_PAGE_INDEX,
                (String) request.getSession(  ).getAttribute( SESSION_CURRENT_PAGE_INDEX ) );

        Map<String, Object> model = new HashMap<String, Object>(  );
        model = getModel(  );

        String strFirstName = request.getParameter( PARAMETER_FIRSTNAME );
        String strLastName = request.getParameter( PARAMETER_LASTNAME );
        String strNemberPhone = request.getParameter( PARAMETER_PHONE );
        String strEmail = request.getParameter( PARAMETER_EMAILM );
        
        String strCustomerId = request.getParameter( PARAMETER_CUSTOMER_ID );
        String strUserIdOpam = request.getParameter( PARAMETER_USER_ID_OPAM );
        
        model.put(PARAMETER_CUSTOMER_ID,strCustomerId );
        model.put( PARAMETER_USER_ID_OPAM,strUserIdOpam);
        
        model.put(PARAMETER_FIRSTNAME,strFirstName);
        model.put(PARAMETER_LASTNAME,strLastName);
        model.put(PARAMETER_PHONE,strNemberPhone);
        model.put( PARAMETER_EMAILM,strEmail);

        if ( strCurrentPageIndex == null )
        {
            strCurrentPageIndex = DEFAULT_CURRENT_PAGE;
        }

        request.getSession(  ).setAttribute( SESSION_CURRENT_PAGE_INDEX, strCurrentPageIndex );

        int nItemsPerPage = Paginator.getItemsPerPage( request, Paginator.PARAMETER_ITEMS_PER_PAGE,
                getIntSessionAttribute( request.getSession(  ), SESSION_ITEMS_PER_PAGE ), _nDefaultItemsPerPage );
        request.getSession(  ).setAttribute( SESSION_ITEMS_PER_PAGE, nItemsPerPage );

        request.getSession(  ).removeAttribute( SESSION_ATTRIBUTE_APPOINTMENT_FORM );

        UrlItem url = new UrlItem( JSP_MANAGE_APPOINTMENTFORMS );
        String strUrl = url.getUrl(  );
        List<AppointmentForm> listAppointmentForms = AppointmentFormHome.getAppointmentFormsList(  );

        // PAGINATOR
        LocalizedPaginator<AppointmentForm> paginator = new LocalizedPaginator<AppointmentForm>( listAppointmentForms,
                nItemsPerPage, strUrl, PARAMETER_PAGE_INDEX, strCurrentPageIndex, getLocale(  ) );

        model.put( MARK_NB_ITEMS_PER_PAGE, Integer.toString( nItemsPerPage ) );
        model.put( MARK_PAGINATOR, paginator );

        model.put( MARK_APPOINTMENTFORM_LIST,
            RBACService.getAuthorizedCollection( paginator.getPageItems(  ),
                AppointmentResourceIdService.PERMISSION_VIEW_FORM, AdminUserService.getAdminUser( request ) ) );

        model.put( VIEW_PERMISSIONS_FORM,
            getPermissions( paginator.getPageItems(  ), AdminUserService.getAdminUser( request ) ) );

        return getPage( PROPERTY_PAGE_TITLE_MANAGE_APPOINTMENTFORMS, TEMPLATE_MANAGE_APPOINTMENTFORMSFIRSTSTEP, model );
    }

    /**
     * Get Form Permissions
     * @param listForms the list form
     * @param user the user
     * @return Form Permissions
     */
    private static String[][] getPermissions( List<AppointmentForm> listForms, AdminUser user )
    {
        String[][] retour = new String[listForms.size(  )][4];
        int nI = 0;

        for ( AppointmentForm tmpForm : listForms )
        {
            String[] strRetour = new String[4];
            strRetour[0] = String.valueOf( RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE,
                        String.valueOf( tmpForm.getIdForm(  ) ), AppointmentResourceIdService.PERMISSION_CREATE_FORM,
                        user ) );
            strRetour[1] = String.valueOf( RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE,
                        String.valueOf( tmpForm.getIdForm(  ) ), AppointmentResourceIdService.PERMISSION_CHANGE_STATE,
                        user ) );
            strRetour[2] = String.valueOf( RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE,
                        String.valueOf( tmpForm.getIdForm(  ) ), AppointmentResourceIdService.PERMISSION_MODIFY_FORM,
                        user ) );
            strRetour[3] = String.valueOf( RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE,
                        String.valueOf( tmpForm.getIdForm(  ) ), AppointmentResourceIdService.PERMISSION_DELETE_FORM,
                        user ) );
            retour[nI++] = strRetour;
        }

        return retour;
    }

    /**
     * Returns the form to create an appointment form
     *
     * @param request The HTTP request
     * @return the HTML code of the appointment form
     * @throws AccessDeniedException If the user is not authorized to create
     *             appointment forms
     */
    @View( VIEW_CREATE_APPOINTMENTFORM )
    public String getCreateAppointmentForm( HttpServletRequest request )
        throws AccessDeniedException
    {
        AppointmentForm appointmentForm = (AppointmentForm) request.getSession(  )
                                                                   .getAttribute( SESSION_ATTRIBUTE_APPOINTMENT_FORM );

        if ( !RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE, "0",
                    AppointmentResourceIdService.PERMISSION_CREATE_FORM, AdminUserService.getAdminUser( request ) ) )
        {
            throw new AccessDeniedException( AppointmentResourceIdService.PERMISSION_CREATE_FORM );
        }

        if ( ( appointmentForm == null ) || ( appointmentForm.getIdForm(  ) > 0 ) )
        {
            appointmentForm = new AppointmentForm(  );
            appointmentForm.setAllowUsersToCancelAppointments( true );
            request.getSession(  ).setAttribute( SESSION_ATTRIBUTE_APPOINTMENT_FORM, appointmentForm );
        }

        Map<String, Object> model = getModel(  );
        model.put( MARK_REF_LIST_CALENDAR_TEMPLATES, CalendarTemplateHome.findAllInReferenceList(  ) );
        model.put( MARK_LOCALE, getLocale(  ) );
        model.put( MARK_LOCALE_TINY, getLocale(  ) );
        addElementsToModelForLeftColumn( request, appointmentForm, getUser(  ), getLocale(  ), model );

        //        model.put( MARK_LOCALE, AppointmentPlugin.getPluginLocale( getLocale( ) ) );
        return getPage( PROPERTY_PAGE_TITLE_CREATE_APPOINTMENTFORM, TEMPLATE_CREATE_APPOINTMENTFORM, model );
    }

    /**
     * Process the data capture form of a new appointment form
     * @param request The HTTP Request
     * @return The JSP URL of the process result
     * @throws AccessDeniedException If the user is not authorized to create
     *             appointment forms
     * @throws FileNotFoundException
     */
    @Action( ACTION_CREATE_APPOINTMENTFORM )
    public String doCreateAppointmentForm( HttpServletRequest request )
        throws AccessDeniedException, FileNotFoundException
    {
        AppointmentForm appointmentForm = (AppointmentForm) request.getSession(  )
                                                                   .getAttribute( SESSION_ATTRIBUTE_APPOINTMENT_FORM );

        if ( !RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE, "0",
                    AppointmentResourceIdService.PERMISSION_CREATE_FORM, AdminUserService.getAdminUser( request ) ) )
        {
            throw new AccessDeniedException( AppointmentResourceIdService.PERMISSION_CREATE_FORM );
        }

        if ( appointmentForm == null )
        {
            appointmentForm = new AppointmentForm(  );
        }

        MultipartHttpServletRequest mRequest = (MultipartHttpServletRequest) request;

        FileItem item = mRequest.getFile( PARAMETER_ICON_RESSOURCE );
        ImageResource img = new ImageResource(  );

        if ( ( item != null ) && ( item.getName(  ) != null ) && !EMPTY_STRING.equals( item.getName(  ) ) )
        {
            byte[] bytes = item.get(  );
            String strMimeType = item.getContentType(  );

            img.setImage( bytes );
            img.setMimeType( strMimeType );

            appointmentForm.setIcon( img );
        }
        else
        {
            img.setImage( new byte[] {  } );
            img.setMimeType( MARK_NULL );
            appointmentForm.setIcon( img );
        }

        if ( checkValidDate( request.getParameter( PARAMETER_ID_START ) ) &&
                checkValidDate( request.getParameter( PARAMETER_ID_END ) ) )
        {
            populate( appointmentForm, request );
            populateAddress( appointmentForm, request );
        }
        else
        {
            return redirectView( request, VIEW_CREATE_APPOINTMENTFORM );
        }

        // Check constraints
        if ( !validateBean( appointmentForm, VALIDATION_ATTRIBUTES_PREFIX ) )
        {
            return redirectView( request, VIEW_CREATE_APPOINTMENTFORM );
        }
       
        try {
			if ( !checkConstraints( appointmentForm ) )
			{
			    return redirect( request, VIEW_CREATE_APPOINTMENTFORM, PARAMETER_ID_FORM, appointmentForm.getIdForm(  ) );
			}
		} catch (ParseException e) {
			
			addError( ERROR_MESSAGE_APPOINTMENT_PARSING_DATE );
			return redirect( request, VIEW_CREATE_APPOINTMENTFORM, PARAMETER_ID_FORM, appointmentForm.getIdForm(  ) );
		}

        if ( appointmentForm.getTimeStart(  ).compareTo( appointmentForm.getTimeEnd(  ) ) >= 0 )
        {
            addError( ERROR_MESSAGE_TIME_START_AFTER_TIME_END, getLocale(  ) );

            return redirectView( request, VIEW_CREATE_APPOINTMENTFORM );
        }

        if ( ( appointmentForm.getDateLimit(  ) == null ) && ( appointmentForm.getNbWeeksToDisplay(  ) == 0 ) )
        {
            addError( MESSAGE_ERROR_DATE_LIMIT_NB_WEEK_EMPTY  , getLocale());

            return redirectView( request, VIEW_CREATE_APPOINTMENTFORM );
        }

        AppointmentFormHome.create( appointmentForm, _appointmentFormService.getDefaultAppointmentFormMessage(  ) );

        AppointmentSlotService.getInstance(  ).computeAndCreateSlotsForForm( appointmentForm );
        AppointmentService.getService(  ).notifyAppointmentFormModified( appointmentForm.getIdForm() );

        request.getSession(  ).removeAttribute( SESSION_ATTRIBUTE_APPOINTMENT_FORM );
        addInfo( INFO_APPOINTMENTFORM_CREATED, getLocale(  ) );

        return redirectView( request, VIEW_MANAGE_APPOINTMENTFORMS );
    }

    private void populateAddress(AppointmentForm appointmentForm,
            HttpServletRequest request)
    {
            String strGeolocAddress = request.getParameter ( PARAMETER_GEOLOC_ADDRESS );
            String strGeolocLatitude = request.getParameter ( PARAMETER_GEOLOC_LATITUDE );
            String strGeolocLongitude = request.getParameter ( PARAMETER_GEOLOC_LONGITUDE );

            //If missing from the form, don't do anything (like populate does)
            if ( strGeolocAddress == null || strGeolocAddress == null || strGeolocLongitude == null )
            {
                return;
            }

            if ( StringUtils.isNotBlank( strGeolocAddress ) &&
                 StringUtils.isNotBlank( strGeolocLatitude ) &&
                 StringUtils.isNotBlank( strGeolocLongitude ))
            {
                appointmentForm.setAddress( strGeolocAddress );
                appointmentForm.setLatitude( Double.valueOf ( strGeolocLatitude ) );
                appointmentForm.setLongitude( Double.valueOf( strGeolocLongitude ) );
            }
            else
            {
                appointmentForm.setAddress( null );
                appointmentForm.setLatitude( null );
                appointmentForm.setLongitude( null );
            }
    }

    /**
     * Manages the removal form of a appointment form whose identifier is in the
     * HTTP request
     * @param request The HTTP request
     * @return the HTML code to confirm
     * @throws AccessDeniedException If the user is not authorized to delete
     *             this appointment form
     */
    @Action( ACTION_CONFIRM_REMOVE_APPOINTMENTFORM )
    public String getConfirmRemoveAppointmentForm( HttpServletRequest request )
        throws AccessDeniedException
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_FORM ) );

        if ( !RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE, request.getParameter( PARAMETER_ID_FORM ),
                    AppointmentResourceIdService.PERMISSION_DELETE_FORM, AdminUserService.getAdminUser( request ) ) )
        {
            throw new AccessDeniedException( AppointmentResourceIdService.PERMISSION_DELETE_FORM );
        }

        UrlItem url = new UrlItem( getActionUrl( ACTION_REMOVE_APPOINTMENTFORM ) );
        url.addParameter( PARAMETER_ID_FORM, nId );

        String strMessageUrl = AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_REMOVE_APPOINTMENTFORM,
                url.getUrl(  ), AdminMessage.TYPE_CONFIRMATION );

        return redirect( request, strMessageUrl );
    }

    /**
     * Handles the removal form of an appointment form
     * @param request The HTTP request
     * @return the JSP URL to display the form to manage appointment forms
     * @throws AccessDeniedException If the user is not authorized to delete
     *             this appointment form
     */
    @Action( ACTION_REMOVE_APPOINTMENTFORM )
    public String doRemoveAppointmentForm( HttpServletRequest request )
        throws AccessDeniedException
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_FORM ) );

        if ( !RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE, request.getParameter( PARAMETER_ID_FORM ),
                    AppointmentResourceIdService.PERMISSION_DELETE_FORM, AdminUserService.getAdminUser( request ) ) )
        {
            throw new AccessDeniedException( AppointmentResourceIdService.PERMISSION_DELETE_FORM );
        }

        _entryService.removeEntriesByIdAppointmentForm( nId );

        AppointmentFormHome.remove( nId );
        AppointmentService.getService(  ).notifyAppointmentFormModified( nId );
        addInfo( INFO_APPOINTMENTFORM_REMOVED, getLocale(  ) );

        return redirectView( request, VIEW_MANAGE_APPOINTMENTFORMS );
    }

    /**
     * Returns the form to update info about a appointment form
     * @param request The HTTP request
     * @return The HTML form to update info
     * @throws AccessDeniedException If the user is not authorized to modify
     *             this appointment form
     */
    @View( VIEW_MODIFY_APPOINTMENTFORM )
    public String getModifyAppointmentForm( HttpServletRequest request )
        throws AccessDeniedException
    {
        AppointmentForm appointmentForm = (AppointmentForm) request.getSession(  )
                                                                   .getAttribute( SESSION_ATTRIBUTE_APPOINTMENT_FORM );

        int nIdForm = Integer.parseInt( request.getParameter( PARAMETER_ID_FORM ) );
        String strPage = request.getParameter( MARK_PAGE );

        if ( ( appointmentForm == null ) || ( nIdForm != appointmentForm.getIdForm(  ) ) ||
                Boolean.parseBoolean( request.getParameter( PARAMETER_FORCE_RELOAD ) ) )
        {
            appointmentForm = AppointmentFormHome.findByPrimaryKey( nIdForm );
            request.getSession(  ).setAttribute( SESSION_ATTRIBUTE_APPOINTMENT_FORM, appointmentForm );
        }

        if ( !RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE,
                    Integer.toString( appointmentForm.getIdForm(  ) ),
                    AppointmentResourceIdService.PERMISSION_MODIFY_FORM, AdminUserService.getAdminUser( request ) ) )
        {
            throw new AccessDeniedException( AppointmentResourceIdService.PERMISSION_MODIFY_FORM );
        }

        EntryFilter entryFilter = new EntryFilter(  );
        entryFilter.setIdResource( appointmentForm.getIdForm(  ) );
        entryFilter.setResourceType( AppointmentForm.RESOURCE_TYPE );
        entryFilter.setEntryParentNull( EntryFilter.FILTER_TRUE );
        entryFilter.setFieldDependNull( EntryFilter.FILTER_TRUE );

        List<Entry> listEntryFirstLevel = EntryHome.getEntryList( entryFilter );
        List<Entry> listEntry = new ArrayList<Entry>( listEntryFirstLevel.size(  ) );

        //        Map<Integer, Integer> mapGroupItemsNumber = new HashMap<Integer, Integer>( );
        List<Integer> listOrderFirstLevel = new ArrayList<Integer>( listEntryFirstLevel.size(  ) );

        for ( Entry entry : listEntryFirstLevel )
        {
            listEntry.add( entry );
            // If the entry is a group, we add entries associated with this group
            listOrderFirstLevel.add( listEntry.size(  ) );

            if ( entry.getEntryType(  ).getGroup(  ) )
            {
                entryFilter = new EntryFilter(  );
                entryFilter.setIdResource( appointmentForm.getIdForm(  ) );
                entryFilter.setResourceType( AppointmentForm.RESOURCE_TYPE );
                entryFilter.setFieldDependNull( EntryFilter.FILTER_TRUE );
                entryFilter.setIdEntryParent( entry.getIdEntry(  ) );

                List<Entry> listEntryGroup = EntryHome.getEntryList( entryFilter );
                entry.setChildren( listEntryGroup );
                //                mapGroupItemsNumber.put( entry.getIdEntry( ), listEntryGroup.size( ) );
                listEntry.addAll( listEntryGroup );
            }
        }

        Map<String, Object> model = getModel(  );
        model.put( MARK_GROUP_ENTRY_LIST, getRefListGroups( appointmentForm.getIdForm(  ) ) );
        model.put( MARK_ENTRY_TYPE_LIST, EntryTypeService.getInstance(  ).getEntryTypeReferenceList(  ) );
        model.put( MARK_ENTRY_LIST, listEntry );
        model.put( MARK_LOCALE, getLocale(  ) );
        model.put( MARK_LOCALE_TINY, getLocale(  ) );
        model.put( MARK_LIST_ORDER_FIRST_LEVEL, listOrderFirstLevel );
        addElementsToModelForLeftColumn( request, appointmentForm, getUser(  ), getLocale(  ), model );

        //        model.put( MARK_MAP_CHILD, mapGroupItemsNumber );
        if ( ( strPage != null ) && strPage.equals( PARAMETER_FORM_RDV ) )
        {
            return getPage( PROPERTY_PAGE_TITLE_MODIFY_APPOINTMENT_FORM, TEMPLATE_MODIFY_APPOINTMENT_FORM, model );
        }
        else
        {
            return getPage( PROPERTY_PAGE_TITLE_MODIFY_APPOINTMENTFORM, TEMPLATE_MODIFY_APPOINTMENTFORM, model );
        }
    }

    /**
     * Returns the form to update info about a appointment form
     * @param request The HTTP request
     * @return The HTML form to update info
     * @throws AccessDeniedException If the user is not authorized to modify
     *             this appointment form
     */
    @View( VIEW_ADVANCED_MODIFY_APPOINTMENTFORM )
    public String getModifyAppointmentFormAdvanced( HttpServletRequest request )
        throws AccessDeniedException
    {
        AppointmentForm appointmentForm = (AppointmentForm) request.getSession(  )
                                                                   .getAttribute( SESSION_ATTRIBUTE_APPOINTMENT_FORM );

        int nIdForm = Integer.parseInt( request.getParameter( PARAMETER_ID_FORM ) );

        if ( ( appointmentForm == null ) || ( nIdForm != appointmentForm.getIdForm(  ) ) ||
                Boolean.parseBoolean( request.getParameter( PARAMETER_FORCE_RELOAD ) ) )
        {
            appointmentForm = AppointmentFormHome.findByPrimaryKey( nIdForm );
            request.getSession(  ).setAttribute( SESSION_ATTRIBUTE_APPOINTMENT_FORM, appointmentForm );
        }

        if ( !RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE,
                    Integer.toString( appointmentForm.getIdForm(  ) ),
                    AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM,
                    AdminUserService.getAdminUser( request ) ) )
        {
            throw new AccessDeniedException( AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM );
        }

        EntryFilter entryFilter = new EntryFilter(  );
        entryFilter.setIdResource( appointmentForm.getIdForm(  ) );
        entryFilter.setResourceType( AppointmentForm.RESOURCE_TYPE );
        entryFilter.setEntryParentNull( EntryFilter.FILTER_TRUE );
        entryFilter.setFieldDependNull( EntryFilter.FILTER_TRUE );

        List<Entry> listEntryFirstLevel = EntryHome.getEntryList( entryFilter );
        List<Entry> listEntry = new ArrayList<Entry>( listEntryFirstLevel.size(  ) );

        //        Map<Integer, Integer> mapGroupItemsNumber = new HashMap<Integer, Integer>( );
        List<Integer> listOrderFirstLevel = new ArrayList<Integer>( listEntryFirstLevel.size(  ) );

        for ( Entry entry : listEntryFirstLevel )
        {
            listEntry.add( entry );
            // If the entry is a group, we add entries associated with this group
            listOrderFirstLevel.add( listEntry.size(  ) );

            if ( entry.getEntryType(  ).getGroup(  ) )
            {
                entryFilter = new EntryFilter(  );
                entryFilter.setIdResource( appointmentForm.getIdForm(  ) );
                entryFilter.setResourceType( AppointmentForm.RESOURCE_TYPE );
                entryFilter.setFieldDependNull( EntryFilter.FILTER_TRUE );
                entryFilter.setIdEntryParent( entry.getIdEntry(  ) );

                List<Entry> listEntryGroup = EntryHome.getEntryList( entryFilter );
                entry.setChildren( listEntryGroup );
                //                mapGroupItemsNumber.put( entry.getIdEntry( ), listEntryGroup.size( ) );
                listEntry.addAll( listEntryGroup );
            }
        }

        Map<String, Object> model = getModel(  );
        model.put( MARK_GROUP_ENTRY_LIST, getRefListGroups( appointmentForm.getIdForm(  ) ) );
        model.put( MARK_ENTRY_TYPE_LIST, EntryTypeService.getInstance(  ).getEntryTypeReferenceList(  ) );
        model.put( MARK_ENTRY_LIST, listEntry );
        model.put( MARK_LOCALE, getLocale(  ) );
        model.put( MARK_LOCALE_TINY, getLocale(  ) );
        model.put( MARK_LIST_ORDER_FIRST_LEVEL, listOrderFirstLevel );
        addElementsToModelForLeftColumn( request, appointmentForm, getUser(  ), getLocale(  ), model );

        //        model.put( MARK_MAP_CHILD, mapGroupItemsNumber );
        return getPage( PROPERTY_PAGE_TITLE_MODIFY_APPOINTMENTFORM, TEMPLATE_ADVANCED_MODIFY_APPOINTMENTFORM, model );
    }

    /**
     * Process the change form of a appointment form
     * @param request The HTTP request
     * @return The JSP URL of the process result
     * @throws AccessDeniedException If the user is not authorized to modify
     *             this appointment form
     */
    @SuppressWarnings( "deprecation" )
    @Action( ACTION_MODIFY_APPOINTMENTFORM )
    public String doModifyAppointmentForm( HttpServletRequest request )
        throws AccessDeniedException
    {
        AppointmentForm appointmentForm = (AppointmentForm) request.getSession(  )
                                                                   .getAttribute( SESSION_ATTRIBUTE_APPOINTMENT_FORM );
        String strIdForm = request.getParameter( PARAMETER_ID_FORM );
        int nIdAppointmentForm = Integer.parseInt( strIdForm );

        java.sql.Date dateMin = null;

        if ( ( appointmentForm == null ) || ( nIdAppointmentForm != appointmentForm.getIdForm(  ) ) )
        {
            appointmentForm = AppointmentFormHome.findByPrimaryKey( nIdAppointmentForm );
        }

        String strForm = request.getParameter( PARAMETER_NAME_FORM );
        String strDeleteIcon = ( request.getParameter( PARAMETER_DELETE_ICON ) == null ) ? MARK_FALSE
                                                                                         : request.getParameter( PARAMETER_DELETE_ICON );

        AppointmentForm appointmentFormTmp = AppointmentFormHome.findByPrimaryKey( appointmentForm.getIdForm(  ) );

        if ( strForm.equals( PARAMETER_SECOND_FORM ) && !strForm.isEmpty(  ) )
        {
            MultipartHttpServletRequest mRequest = (MultipartHttpServletRequest) request;
            FileItem item = mRequest.getFile( PARAMETER_ICON_RESSOURCE );

            if ( ( item != null ) && ( item.getName(  ) != null ) && !EMPTY_STRING.equals( item.getName(  ) ) )
            {
                byte[] bytes = item.get(  );
                String strMimeType = item.getContentType(  );
                ImageResource img = new ImageResource(  );

                img.setImage( bytes );
                img.setMimeType( strMimeType );
                appointmentForm.setIcon( img );
            }

            if ( Boolean.parseBoolean( strDeleteIcon ) && ( appointmentForm.getIcon(  ).getImage(  ) != null ) )
            {
                ImageResource img = new ImageResource(  );
                img.setImage( null );
                img.setMimeType( null );
                appointmentForm.setIcon( img );
            }
        }

        if ( !RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE,
                    Integer.toString( appointmentForm.getIdForm(  ) ),
                    AppointmentResourceIdService.PERMISSION_MODIFY_FORM, AdminUserService.getAdminUser( request ) ) )
        {
            throw new AccessDeniedException( AppointmentResourceIdService.PERMISSION_MODIFY_FORM );
        }

        if ( checkValidDate( request.getParameter( PARAMETER_ID_START ) ) &&
                checkValidDate( request.getParameter( PARAMETER_ID_END ) ) )
        {
            populate( appointmentForm, request );
            populateAddress( appointmentForm, request );

            if ( strForm.equals( PARAMETER_SECOND_FORM ) && !strForm.isEmpty(  ) )
            {
                setParametersDays( appointmentForm, appointmentFormTmp );
            }

            if ( strForm.equals( PARAMETER_FIRST_FORM ) && !strForm.isEmpty(  ) )
            {
                setAlterablesParameters( appointmentForm, appointmentFormTmp );
            }
        }
        else
        {
            return redirect( request, VIEW_MODIFY_APPOINTMENTFORM, PARAMETER_ID_FORM, appointmentForm.getIdForm(  ) );
        }

        // Check constraints
        /*if ( !validateBean( appointmentForm, VALIDATION_ATTRIBUTES_PREFIX ) )
        {
            if ( strForm.equals( PARAMETER_FIRST_FORM ) )
            {
                return redirect( request, VIEW_ADVANCED_MODIFY_APPOINTMENTFORM, PARAMETER_ID_FORM,
                    appointmentForm.getIdForm(  ) );
            }
            else
            {
                return redirect( request, VIEW_MODIFY_APPOINTMENTFORM, PARAMETER_ID_FORM, appointmentForm.getIdForm(  ) );
            }
        }*/

        //Check Constraint better
        try
        {
            if ( !checkConstraints( appointmentForm ) )
            {
                return redirect( request, VIEW_MODIFY_APPOINTMENTFORM, PARAMETER_ID_FORM, appointmentForm.getIdForm(  ) );
            }
        }
        catch ( ParseException e )
        {
            addError( ERROR_MESSAGE_APPOINTMENT_PARSING_DATE );

            return redirect( request, VIEW_MODIFY_APPOINTMENTFORM, PARAMETER_ID_FORM, appointmentForm.getIdForm(  ) );
        }

        if ( ( appointmentForm.getDateLimit(  ) == null ) && ( appointmentForm.getNbWeeksToDisplay(  ) == 0 ) )
        {
            addError( MESSAGE_ERROR_DATE_LIMIT_NB_WEEK_EMPTY , getLocale());

            return redirect( request, VIEW_MODIFY_APPOINTMENTFORM, PARAMETER_ID_FORM, appointmentForm.getIdForm(  ) );
        }

        AppointmentForm formFromDb = AppointmentFormHome.findByPrimaryKey( appointmentForm.getIdForm(  ) );

        if(formFromDb.getDateLimit() != null && appointmentForm.getNbWeeksToDisplay() > 0){
        	    appointmentForm.setDateLimit(null);
        }
        if(formFromDb.getNbWeeksToDisplay() > 0 && appointmentForm.getDateLimit() != null){
        	appointmentForm.setNbWeeksToDisplay(0);
        }
        // The populate set the active flag to false, so we have to restore it
        appointmentForm.setIsActive( formFromDb.getIsActive(  ) );
        AppointmentFormHome.update( appointmentForm );

        if ( AppointmentSlotService.getInstance(  ).checkForFormModification( appointmentForm, formFromDb ) )
        {
            addInfo( INFO_MODIFY_APPOINTMENTFORM_SLOTS_UPDATED, getLocale(  ) );
        }
        
        if(!strForm.equals( PARAMETER_FIRST_FORM ) && !strForm.isEmpty(  )){
        	
        	AppointmentService.getService().checkFormDays(appointmentForm, false);
        }

        if ( strForm.equals( PARAMETER_FIRST_FORM ) && !strForm.isEmpty(  ) )
        {
            if ( !RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE, strIdForm,
                        AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM, getUser(  ) ) )
            {
                throw new AccessDeniedException( AppointmentResourceIdService.PERMISSION_MODIFY_ADVANCED_SETTING_FORM );
            }

            String strDateMin = ( request.getParameter( PARAMETER_DATE_MIN ) == null ) ? StringUtils.EMPTY
                                                                                       : request.getParameter( PARAMETER_DATE_MIN );

            if ( !strDateMin.isEmpty(  ) )
            {
                dateMin = new java.sql.Date( DateUtil.getDate( strDateMin ).getTime(  ) );
            }

            int nNbAppointments = AppointmentHome.countAppointmentsByIdForm( nIdAppointmentForm, dateMin );
            
            if ( appointmentForm.getMaximumNumberOfBookedSeats() > appointmentForm.getPeoplePerAppointment() )
            {
                addError( MESSAGE_ERROR_NUMBER_OF_SEATS_BOOKED, getLocale(  ) );
                
                return redirect( request, VIEW_ADVANCED_MODIFY_APPOINTMENTFORM, PARAMETER_ID_FORM,
                        appointmentForm.getIdForm(  ) );
            }

            if ( strDateMin.isEmpty(  ) )
            {
                addError( MESSAGE_ERROR_START_DATE_EMPTY, getLocale(  ) );

                return redirect( request, VIEW_ADVANCED_MODIFY_APPOINTMENTFORM, PARAMETER_ID_FORM,
                    appointmentForm.getIdForm(  ) );
            }

            if ( nNbAppointments > 0 )
            {
                addError( MESSAGE_ERROR_MODIFY_FORM_HAS_APPOINTMENTS, getLocale(  ) );

                return redirect( request, VIEW_ADVANCED_MODIFY_APPOINTMENTFORM, PARAMETER_ID_FORM,
                    appointmentForm.getIdForm(  ) );
            }

            AppointmentService.getService(  )
                              .resetFormDays( AppointmentFormHome.findByPrimaryKey( nIdAppointmentForm ), dateMin, false );
        }

        AppointmentService.getService(  ).notifyAppointmentFormModified( nIdAppointmentForm );
        request.getSession(  ).removeAttribute( SESSION_ATTRIBUTE_APPOINTMENT_FORM );
        addInfo( INFO_APPOINTMENTFORM_UPDATED, getLocale(  ) );

        return redirectView( request, VIEW_MANAGE_APPOINTMENTFORMS );
    }

    private void setParametersDays( AppointmentForm appointmentForm, AppointmentForm appointmentFormTmp )
    {
        appointmentForm.setIsOpenMonday( appointmentFormTmp.getIsOpenMonday(  ) );
        appointmentForm.setIsOpenTuesday( appointmentFormTmp.getIsOpenTuesday(  ) );
        appointmentForm.setIsOpenWednesday( appointmentFormTmp.getIsOpenWednesday(  ) );
        appointmentForm.setIsOpenThursday( appointmentFormTmp.getIsOpenThursday(  ) );
        appointmentForm.setIsOpenFriday( appointmentFormTmp.getIsOpenFriday(  ) );
        appointmentForm.setIsOpenSaturday( appointmentFormTmp.getIsOpenSaturday(  ) );
        appointmentForm.setIsOpenSunday( appointmentFormTmp.getIsOpenSunday(  ) );
    }

    private void setAlterablesParameters( AppointmentForm appointmentForm, AppointmentForm appointmentFormTmp )
    {
        appointmentForm.setAllowUsersToCancelAppointments( appointmentFormTmp.getAllowUsersToCancelAppointments(  ) );
        appointmentForm.setDisplayTitleFo( appointmentFormTmp.getDisplayTitleFo(  ) );
        appointmentForm.setIsFormStep( appointmentFormTmp.getIsFormStep(  ) );
        appointmentForm.setEnableCaptcha( appointmentFormTmp.getEnableCaptcha(  ) );
        appointmentForm.setEnableConfirmEmail( appointmentFormTmp.getEnableConfirmEmail(  ) );
        appointmentForm.setEnableMandatoryEmail( appointmentFormTmp.getEnableMandatoryEmail(  ) );
    }

    /**
     * Get the URL to manage appointment forms
     * @param request The request
     * @param strIdForm The id of the form to manage days of
     * @return The URL to manage appointment forms
     */
    private static String getURLManageAppointmentFormDays( HttpServletRequest request, String strIdForm )
    {
        UrlItem urlItem = new UrlItem( AppPathService.getBaseUrl( request ) + JSP_MANAGE_APPOINTMENTFORMS );
        urlItem.addParameter( MVCUtils.PARAMETER_VIEW, VIEW_MODIFY_APPOINTMENTFORM );
        urlItem.addParameter( PARAMETER_ID_FORM, strIdForm );

        return urlItem.getUrl(  );
    }

    /**
     * Check Constraints
     * @param appointmentForm
     * @return
     * @throws ParseException
     */
    private boolean checkConstraints( AppointmentForm appointmentForm )
        throws ParseException
    {
        boolean bReturn = true;
        SimpleDateFormat formatterHHMM = new SimpleDateFormat( "HH:mm", Locale.FRENCH );
        Date dtDate1 = (Date) formatterHHMM.parse( appointmentForm.getTimeStart(  ).replace( 'h', ':' ) );
        Date dtDate2 = (Date) formatterHHMM.parse( appointmentForm.getTimeEnd(  ).replace( 'h', ':' ) );

        if ( dtDate1.after( dtDate2 ) )
        {
            bReturn = false;
            addError( ERROR_MESSAGE_TIME_START_AFTER_TIME_END, getLocale(  ) );
        }

        if ( appointmentForm.getMinDaysBeforeAppointment(  ) < 0 )
        {
            bReturn = false;
            addError( ERROR_MESSAGE_APPOINTMENT_FAILED, getLocale(  ) );
        }

        if ( ( appointmentForm.getDateStartValidity(  ) != null ) && ( appointmentForm.getDateEndValidity(  ) != null ) )
        {
            if ( appointmentForm.getDateStartValidity(  ).after( appointmentForm.getDateEndValidity(  ) ) )
            {
                bReturn = false;
                addError( ERROR_MESSAGE_TIME_START_AFTER_DATE_END, getLocale(  ) );
            }
            else
            {
                long lMinutes = getDateDiff( dtDate1, dtDate2, TimeUnit.MINUTES );

                if ( appointmentForm.getMinDaysBeforeAppointment(  ) > lMinutes )
                {
                    bReturn = false;
                    addError( ERROR_MESSAGE_APPOINTMENT_SUPERIOR, getLocale(  ) );
                }

                if ( appointmentForm.getDurationAppointments(  ) > lMinutes )
                {
                    bReturn = false;
                    addError( ERROR_MESSAGE_APPOINTMENT_SUPERIOR_MIDDLE, getLocale(  ) );
                }
                else
                {
                    if ( ( lMinutes % appointmentForm.getDurationAppointments(  ) ) != 0 ) //Duration Time must be a modulo
                    {
                        addError( MESSAGE_ERROR_DAY_DURATION_APPOINTMENT_NOT_MULTIPLE_FORM, getLocale(  ) );
                    }
                }
            }
        }
        /*if ( appointmentForm.getMaximumNumberOfBookedSeats() > appointmentForm.getPeoplePerAppointment() )
        {
            bReturn = false;
            addError( MESSAGE_ERROR_NUMBER_OF_SEATS_BOOKED, getLocale(  ) );
        }*/

        return bReturn;
    }

    /**
     * @param request
     * @return
     */
    private boolean checkValidDate( String strDate )
    {
        boolean bReturn = true;
        Pattern pattern = Pattern.compile( "([012]\\d|3[01])/([012]\\d|3[01])/(19|20)\\d\\d" );

        if ( !StringUtils.isEmpty( strDate ) )
        {
            try
            {
                Matcher m = pattern.matcher( strDate );

                if ( !m.matches(  ) )
                {
                    throw new ParseException( strDate, -1 );
                }

                java.util.Date strMyDate = DateUtil.formatDate( strDate, getLocale(  ) );

                if ( strMyDate == null )
                {
                    throw new ParseException( strDate, -1 );
                }
            }
            catch ( ParseException e )
            {
                addError( ERROR_MESSAGE_APPOINTMENT_DATES, getLocale(  ) );
                bReturn = false;
            }
        }

        return bReturn;
    }

    /**
         * Get a diff between two dates
         * @param date1 the oldest date
         * @param date2 the newest date
         * @param timeUnit the unit in which you want the diff
         * @return the diff value, in the provided unit
         */
    public static long getDateDiff( Date date1, Date date2, TimeUnit timeUnit )
    {
        long diffInMillies = date2.getTime(  ) - date1.getTime(  );

        return timeUnit.convert( diffInMillies, TimeUnit.MILLISECONDS );
    }

    /**
     * Change the enabling of an appointment form
     * @param request The request
     * @return The next URL to redirect to
     * @throws AccessDeniedException If the user is not authorized to change the
     *             activation of this appointment form
     */
    @Action( ACTION_DO_CHANGE_FORM_ACTIVATION )
    public String doChangeFormActivation( HttpServletRequest request )
        throws AccessDeniedException
    {
        String strIdForm = request.getParameter( PARAMETER_ID_FORM );

        if ( StringUtils.isNotEmpty( strIdForm ) && StringUtils.isNumeric( strIdForm ) )
        {
            if ( !RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE, strIdForm,
                        AppointmentResourceIdService.PERMISSION_CHANGE_STATE, AdminUserService.getAdminUser( request ) ) )
            {
                throw new AccessDeniedException( AppointmentResourceIdService.PERMISSION_CHANGE_STATE );
            }

            int nIdForm = Integer.parseInt( strIdForm );
            AppointmentForm form = AppointmentFormHome.findByPrimaryKey( nIdForm );

            // If we enable the form, we check that its days has already been created
            if ( !form.getIsActive(  ) )
            {
                AppointmentService.getService(  ).checkFormDays( form, false );

                // If we enable the form, and it has a passed date of end of validity, we remove it to prevent to form from being disabled by the publication daemon
                if ( ( form.getDateEndValidity(  ) != null ) &&
                        ( form.getDateEndValidity(  ).getTime(  ) < System.currentTimeMillis(  ) ) )
                {
                    form.setDateEndValidity( null );
                }
            }
            else
            {
                // If we disable the form, and it has a passed date of start of validity, we remove it to prevent to form from being enabled by the publication daemon
                if ( ( form.getDateStartValidity(  ) != null ) &&
                        ( form.getDateStartValidity(  ).getTime(  ) < System.currentTimeMillis(  ) ) )
                {
                    form.setDateStartValidity( null );
                }
            }

            form.setIsActive( !form.getIsActive(  ) );
            AppointmentFormHome.update( form );
            AppointmentService.getService(  ).notifyAppointmentFormModified( form.getIdForm() );
        }

        if ( Boolean.valueOf( request.getParameter( PARAMETER_FROM_DASHBOARD ) ) )
        {
            return redirect( request, AppPathService.getBaseUrl( request ) + AppPathService.getAdminMenuUrl(  ) );
        }

        return redirectView( request, VIEW_MANAGE_APPOINTMENTFORMS );
    }

    /**
     * Get the page to modify an appointment form
     * @param request The request
     * @return The HTML content to display
     * @throws AccessDeniedException If the user is not authorized to modify
     *             this appointment form
     */
    @View( VIEW_MODIFY_FORM_MESSAGES )
    public String getModifyAppointmentFormMessages( HttpServletRequest request )
        throws AccessDeniedException
    {
        String strIdForm = request.getParameter( PARAMETER_ID_FORM );

        if ( StringUtils.isNotEmpty( strIdForm ) && StringUtils.isNumeric( strIdForm ) )
        {
            if ( !RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE, strIdForm,
                        AppointmentResourceIdService.PERMISSION_MODIFY_FORM, AdminUserService.getAdminUser( request ) ) )
            {
                throw new AccessDeniedException( AppointmentResourceIdService.PERMISSION_MODIFY_FORM );
            }

            int nIdForm = Integer.parseInt( strIdForm );
            AppointmentFormMessages formMessages = AppointmentFormMessagesHome.findByPrimaryKey( nIdForm );
            Map<String, Object> model = new HashMap<String, Object>(  );
            model.put( MARK_FORM_MESSAGE, formMessages );
            model.put( MARK_WEBAPP_URL, AppPathService.getBaseUrl( request ) );
            model.put( MARK_LOCALE, getLocale(  ) );
            model.put( MARK_LOCALE_TINY, getLocale(  ) );

            return getPage( PROPERTY_PAGE_TITLE_MODIFY_APPOINTMENTFORM_MESSAGES,
                TEMPLATE_MODIFY_APPOINTMENTFORM_MESSAGES, model );
        }

        return redirectView( request, VIEW_MANAGE_APPOINTMENTFORMS );
    }

    /**
     * Do modify an appointment form messages
     * @param request The request
     * @return The next URL to redirect to
     * @throws AccessDeniedException If the user is not authorized to modify
     *             this appointment form
     */
    @Action( ACTION_DO_MODIFY_FORM_MESSAGES )
    public String doModifyAppointmentFormMessages( HttpServletRequest request )
        throws AccessDeniedException
    {
        String strIdForm = request.getParameter( PARAMETER_ID_FORM );

        if ( StringUtils.isNotEmpty( strIdForm ) && StringUtils.isNumeric( strIdForm ) &&
                ( request.getParameter( PARAMETER_BACK ) == null ) )
        {
            if ( !RBACService.isAuthorized( AppointmentForm.RESOURCE_TYPE, strIdForm,
                        AppointmentResourceIdService.PERMISSION_MODIFY_FORM, AdminUserService.getAdminUser( request ) ) )
            {
                throw new AccessDeniedException( AppointmentResourceIdService.PERMISSION_MODIFY_FORM );
            }

            int nIdForm = Integer.parseInt( strIdForm );
            UrlItem url = new UrlItem( getViewFullUrl( VIEW_MODIFY_FORM_MESSAGES ) );
            url.addParameter( PARAMETER_ID_FORM, nIdForm );

            AppointmentFormMessages formMessages = AppointmentFormMessagesHome.findByPrimaryKey( nIdForm );

            populate( formMessages, request );

            AppointmentFormMessagesHome.update( formMessages );

            return redirect( request, VIEW_MODIFY_FORM_MESSAGES, PARAMETER_ID_FORM, nIdForm );
        }

        return redirectView( request, VIEW_MANAGE_APPOINTMENTFORMS );
    }

    /**
     * Get url manage appointment form
     * @param request the request
     * @param strIdForm the id form
     * @return url manage appointment form
     */
    public static String getURLManageAppointmentForm( HttpServletRequest request, String strIdForm )
    {
        UrlItem urlItem = new UrlItem( AppPathService.getBaseUrl( request ) + JSP_MANAGE_APPOINTMENTFORMS );
        urlItem.addParameter( MVCUtils.PARAMETER_VIEW, VIEW_MODIFY_FORM_MESSAGES );
        urlItem.addParameter( PARAMETER_ID_FORM, strIdForm );

        return urlItem.getUrl(  );
    }

    /**
     * Get an integer attribute from the session
     * @param session The session
     * @param strSessionKey The session key of the item
     * @return The value of the attribute, or 0 if the key is not associated
     *         with any value
     */
    private int getIntSessionAttribute( HttpSession session, String strSessionKey )
    {
        Integer nAttr = (Integer) session.getAttribute( strSessionKey );

        if ( nAttr != null )
        {
            return nAttr;
        }

        return 0;
    }

    /**
     * Get the reference list of groups
     * @param nIdForm the id of the appointment form
     * @return The reference list of groups of the given form
     */
    private static ReferenceList getRefListGroups( int nIdForm )
    {
        EntryFilter entryFilter = new EntryFilter(  );
        entryFilter.setIdResource( nIdForm );
        entryFilter.setResourceType( AppointmentForm.RESOURCE_TYPE );
        entryFilter.setIdIsGroup( 1 );

        List<Entry> listEntry = EntryHome.getEntryList( entryFilter );

        ReferenceList refListGroups = new ReferenceList(  );

        for ( Entry entry : listEntry )
        {
            refListGroups.addItem( entry.getIdEntry(  ), entry.getTitle(  ) );
        }

        return refListGroups;
    }

    /**
     * Get the URL to modify an appointment form
     * @param request The request
     * @param nIdForm The id of the form to modify
     * @return The URL to modify the given Appointment form
     */
    public static String getURLModifyAppointmentForm( HttpServletRequest request, int nIdForm )
    {
        return getURLModifyAppointmentForm( request, Integer.toString( nIdForm ) );
    }

    /**
     * Get the URL to modify an appointment form
     * @param request The request
     * @param strIdForm The id of the form to modify
     * @return The URL to modify the given Appointment form
     */
    public static String getURLModifyAppointmentForm( HttpServletRequest request, String strIdForm )
    {
        UrlItem urlItem = new UrlItem( AppPathService.getBaseUrl( request ) + JSP_MANAGE_APPOINTMENTFORMS );
        urlItem.addParameter( MVCUtils.PARAMETER_VIEW, VIEW_MODIFY_APPOINTMENTFORM );
        urlItem.addParameter( PARAMETER_ID_FORM, strIdForm );

        return urlItem.getUrl(  );
    }

    /**
     * Get the URL to manage appointment forms
     * @param request The request
     * @return The URL to manage appointment forms
     */
    public static String getURLManageAppointmentForms( HttpServletRequest request )
    {
        UrlItem urlItem = new UrlItem( AppPathService.getBaseUrl( request ) + JSP_MANAGE_APPOINTMENTFORMS );
        urlItem.addParameter( MVCUtils.PARAMETER_VIEW, VIEW_MANAGE_APPOINTMENTFORMS );

        return urlItem.getUrl(  );
    }

    /**
     * Add elements to the model to display the left column to modify an
     * appointment form
     * @param request The request to store the appointment form in session
     * @param appointmentForm The appointment form
     * @param user The user
     * @param locale The locale
     * @param model the model to add elements in
     */
    public static void addElementsToModelForLeftColumn( HttpServletRequest request, AppointmentForm appointmentForm,
        AdminUser user, Locale locale, Map<String, Object> model )
    {
        model.put( MARK_APPOINTMENTFORM, appointmentForm );
        model.put( MARK_LIST_WORKFLOWS, WorkflowService.getInstance(  ).getWorkflowsEnabled( user, locale ) );
        model.put( MARK_IS_CAPTCHA_ENABLED, _captchaSecurityService.isAvailable(  ) );
        model.put( MARK_REF_LIST_CALENDAR_TEMPLATES, CalendarTemplateHome.findAllInReferenceList(  ) );

        Plugin pluginAppointmentResource = PluginService.getPlugin( AppPropertiesService.getProperty( 
                    PROPERTY_MODULE_APPOINTMENT_RESOURCE_NAME ) );
        model.put( MARK_APPOINTMENT_RESOURCE_ENABLED,
            ( pluginAppointmentResource != null ) && pluginAppointmentResource.isInstalled(  ) );
        request.getSession(  ).setAttribute( SESSION_ATTRIBUTE_APPOINTMENT_FORM, appointmentForm );
    }

    @Action( ACTION_DO_COPY_FORM )
    public String doCopyAppointmentForm( HttpServletRequest request )
    {
        String strIdForm = request.getParameter( PARAMETER_ID_FORM );
        int nIdForm = Integer.parseInt( strIdForm );
        AppointmentForm formCopy = AppointmentFormHome.findByPrimaryKey( nIdForm );

        if ( formCopy != null )
        {
            String newNameForCopy = I18nService.getLocalizedString( PROPERTY_COPY_OF_FORM, request.getLocale(  ) ) +
                formCopy.getTitle(  );
            formCopy.setTitle( newNameForCopy );
            formCopy.setIsActive(false);
            AppointmentFormMessages appointmentFormMessages = AppointmentFormMessagesHome.findByPrimaryKey( nIdForm );
            AppointmentFormHome.create( formCopy, appointmentFormMessages );

            List<AppointmentDay> listDays = AppointmentDayHome.findByIdForm( nIdForm );
            Map<Integer,Integer> mapping = new HashMap<>();
            if ( listDays != null )
            {
                for ( AppointmentDay day : listDays )
                {
                    day.setIdForm( formCopy.getIdForm(  ) );
                    int oldValue = day.getIdDay() ;
                    AppointmentDayHome.create( day );
                    if(!mapping.containsKey(oldValue)){
                    	mapping.put(oldValue, day.getIdDay());
                    }
                }
            }

            List<java.sql.Date> listHolidays = AppointmentHoliDaysHome.findByIdForm( nIdForm );

            if ( listHolidays != null )
            {
                for ( java.sql.Date holidays : listHolidays )
                {
                    AppointmentHoliDaysHome.create( holidays, formCopy.getIdForm(  ) );
                }
            }

            List<AppointmentSlot> listSlot = AppointmentSlotHome.findByIdFormAll( nIdForm );

            if ( listSlot != null )
            {
                for ( AppointmentSlot slot : listSlot )
                {
                    slot.setIdForm( formCopy.getIdForm(  ) );
                    slot.setIdDay((mapping.get(slot.getIdDay()) == null) ? 0 : mapping.get(slot.getIdDay()));
                    AppointmentSlotHome.create( slot );
                }
            }
        }
        EntryFilter filter = new EntryFilter();
        filter.setIdResource(nIdForm);
        List<Entry> listEntry = EntryHome.getEntryList(filter);
        List<Field> listField = null ;
        if(listEntry != null){
        	for (Entry entry : listEntry){
        		 entry.setIdResource(formCopy.getIdForm(  ) );
        		 int oldEntry = entry.getIdEntry() ;
        		 EntryHome.create(entry);
        		 listField = FieldHome.getFieldListByIdEntry( oldEntry );
        		 if (listField != null) {
					for (Field field : listField) {
						field.setParentEntry(entry);
						FieldHome.create(field);
					}
				}
        	}
        	
        	
        }

        return getManageAppointmentForms( request );
    }
}
