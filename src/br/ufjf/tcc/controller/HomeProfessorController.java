package br.ufjf.tcc.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Window;

import br.ufjf.tcc.business.QuestionarioBusiness;
import br.ufjf.tcc.business.TCCBusiness;
import br.ufjf.tcc.business.UsuarioBusiness;
import br.ufjf.tcc.model.Participacao;
import br.ufjf.tcc.model.Questionario;
import br.ufjf.tcc.model.TCC;
import br.ufjf.tcc.model.Usuario;

public class HomeProfessorController extends CommonsController {
	private List<TCC> tccs = new ArrayList<TCC>();
	private Usuario professor = null;
	private Questionario currentQuestionary;
	private boolean currentQuestionaryExists = true, currentQuestionaryUsed = true;

	@Init
	public void init() {
		if (getUsuario().getTipoUsuario().getIdTipoUsuario() < Usuario.PROFESSOR
				&& !checaPermissao("hc__"))
			super.paginaProibida();

		else {
			professor = getUsuario();
			new UsuarioBusiness().update(professor, true, false, true);
			for (Participacao p : professor.getParticipacoes()) {
				new TCCBusiness().update(p.getTcc(), true, true, false);
				tccs.add(p.getTcc());
			}
		}
		
		currentQuestionary = new QuestionarioBusiness().getCurrentQuestionaryByCurso(professor.getCurso());
		
		if (currentQuestionary == null)
			currentQuestionaryExists = false;
		
		currentQuestionaryUsed = new QuestionarioBusiness().isQuestionaryUsed(currentQuestionary);
	}

	public List<TCC> getTccs() {
		return tccs;
	}

	public Usuario getProfessor() {
		return professor;
	}

	public boolean isCurrentQuestionaryExists() {
		return currentQuestionaryExists;
	}

	public boolean isCurrentQuestionaryUsed() {
		return currentQuestionaryUsed;
	}

	@Command
	public void createQuestionary() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("curso", professor.getCurso());
		map.put("editing", false);
		final Window dialog = (Window) Executions.createComponents("/pages/cadastro-questionario.zul",null , map);
		dialog.doModal();
	}
	
	@Command
	public void createQuestionaryFromOld() {
		final Window dialog = (Window) Executions.createComponents("/pages/lista-questionarios.zul",null , null);
		dialog.doModal();
	}
	
	@Command
	public void editQuestionary() {
		new QuestionarioBusiness().update(currentQuestionary, true, true);
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("quest", currentQuestionary);
		map.put("editing", true);
		final Window dialog = (Window) Executions.createComponents("/pages/cadastro-questionario.zul",null , map);
		dialog.doModal();
	}
	
}
