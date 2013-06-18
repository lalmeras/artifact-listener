package fr.openwide.maven.artifact.notifier.web.application.artifact.form;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.odlabs.wiquery.core.events.StateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.openwide.core.wicket.more.markup.html.feedback.FeedbackUtils;
import fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.bootstrap.modal.component.AbstractAjaxModalPopupPanel;
import fr.openwide.core.wicket.more.markup.html.template.js.jquery.plugins.bootstrap.modal.component.DelegatedMarkupPanel;
import fr.openwide.core.wicket.more.model.BindingModel;
import fr.openwide.core.wicket.more.model.GenericEntityModel;
import fr.openwide.maven.artifact.notifier.core.business.artifact.model.Artifact;
import fr.openwide.maven.artifact.notifier.core.business.artifact.model.ArtifactDeprecationStatus;
import fr.openwide.maven.artifact.notifier.core.business.artifact.service.IArtifactService;
import fr.openwide.maven.artifact.notifier.core.util.binding.Binding;
import fr.openwide.maven.artifact.notifier.web.application.artifact.component.ArtifactDeprecationStatusDropDownChoice;
import fr.openwide.maven.artifact.notifier.web.application.artifact.component.ArtifactDropDownChoice;
import fr.openwide.maven.artifact.notifier.web.application.common.form.InputPrerequisiteEnabledBehavior;
import fr.openwide.maven.artifact.notifier.web.application.navigation.util.LinkUtils;

public class ArtifactDeprecationFormPopupPanel extends AbstractAjaxModalPopupPanel<Artifact> {

	private static final long serialVersionUID = 4914283916847151778L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactDeprecationFormPopupPanel.class);

	@SpringBean
	private IArtifactService artifactService;
	
	private Form<Artifact> form;

	public ArtifactDeprecationFormPopupPanel(String id) {
		super(id, new GenericEntityModel<Long, Artifact>(null));
	}
	
	public ArtifactDeprecationFormPopupPanel(String id, IModel<Artifact> artifactModel) {
		super(id, artifactModel);
	}

	@Override
	protected Component createHeader(String wicketId) {
		return new Label(wicketId, new ResourceModel("artifact.deprecation.title"));
	}

	@Override
	protected Component createBody(String wicketId) {
		DelegatedMarkupPanel body = new DelegatedMarkupPanel(wicketId, ArtifactDeprecationFormPopupPanel.class);
		
		form = new Form<Artifact>("form", getModel());
		body.add(form);
		
		final MarkupContainer relatedArtifactContainer = new WebMarkupContainer("relatedArtifactContainer");
		relatedArtifactContainer.setOutputMarkupId(true);
		form.add(relatedArtifactContainer);
		
		final ArtifactDropDownChoice relatedArtifactField = new ArtifactDropDownChoice("relatedArtifact",
				BindingModel.of(form.getModel(), Binding.artifact().relatedArtifact()));
		relatedArtifactField.setLabel(new ResourceModel("artifact.deprecation.field.relatedArtifact"));
		relatedArtifactContainer.add(relatedArtifactField);
		
		ArtifactDeprecationStatusDropDownChoice deprecatedField = new ArtifactDeprecationStatusDropDownChoice("deprecationStatus",
				BindingModel.of(form.getModel(), Binding.artifact().deprecationStatus()));
		deprecatedField.setLabel(new ResourceModel("artifact.deprecation.field.deprecationStatus"));
		
		relatedArtifactField.add(new InputPrerequisiteEnabledBehavior<ArtifactDeprecationStatus>(deprecatedField) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean shouldSetUpAttachedComponent(FormComponent<ArtifactDeprecationStatus> prerequisiteField) {
				if (prerequisiteField.getInput() == null) {
					return ArtifactDeprecationStatus.DEPRECATED.equals(prerequisiteField.getModelObject());
				}
				return super.shouldSetUpAttachedComponent(prerequisiteField) &&
						ArtifactDeprecationStatus.DEPRECATED.equals(prerequisiteField.getConvertedInput());
			}
		});
		deprecatedField.add(new AjaxEventBehavior(StateEvent.CHANGE.getEventLabel()) {
			private static final long serialVersionUID = 1L;
			
			@Override
			protected void onEvent(AjaxRequestTarget target) {
				target.add(relatedArtifactContainer);
			}
		});
		form.add(deprecatedField);

		return body;
	}

	@Override
	protected Component createFooter(String wicketId) {
		DelegatedMarkupPanel footer = new DelegatedMarkupPanel(wicketId, ArtifactDeprecationFormPopupPanel.class);
		
		// Validate button
		AjaxButton validate = new AjaxButton("save", form) {
			private static final long serialVersionUID = 1L;
			
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				Artifact artifact = ArtifactDeprecationFormPopupPanel.this.getModelObject();
				Artifact relatedArtifact = artifact.getRelatedArtifact();
				
				try {
					if (relatedArtifact == null || relatedArtifact.getId() != artifact.getId()) {
						artifactService.update(artifact);
						getSession().success(getString("artifact.deprecation.success"));
						closePopup(target);
						throw new RestartResponseException(getPage().getPageClass(), LinkUtils.getArtifactPageParameters(artifact));
					} else {
						getSession().error(getString("artifact.deprecation.relatedArtifact.equal"));
					}
				} catch (RestartResponseException e) {
					throw e;
				} catch (Exception e) {
					LOGGER.error("Error occured while updating the artifact", e);
					getSession().error(getString("artifact.deprecation.error"));
				}
				FeedbackUtils.refreshFeedback(target, getPage());
			}
			
			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				FeedbackUtils.refreshFeedback(target, getPage());
			}
		};
		validate.add(new Label("validateLabel", new ResourceModel("common.action.save")));
		footer.add(validate);
		
		// Cancel button
		AbstractLink cancel = new AbstractLink("cancel") {
			private static final long serialVersionUID = 1L;
		};
		addCancelBehavior(cancel);
		footer.add(cancel);
		
		return footer;
	}
	
	@Override
	protected IModel<String> getCssClassNamesModel() {
		return Model.of("modal-artifact-deprecation");
	}
}
