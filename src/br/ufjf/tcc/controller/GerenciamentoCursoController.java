package br.ufjf.tcc.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Window;

import br.ufjf.tcc.business.CursoBusiness;
import br.ufjf.tcc.model.Curso;
import br.ufjf.tcc.persistent.impl.CursoDAO;

public class GerenciamentoCursoController extends CommonsController {
	private CursoBusiness cursoBusiness = new CursoBusiness();
	private Curso novoCurso = null;
	private Map<Integer, Curso> editTemp = new HashMap<Integer, Curso>();
	private List<Curso> allCursos = cursoBusiness.getCursos(),
			cursos = allCursos, cursosCSV = new ArrayList<Curso>();
	private String filterString = "";

	@Init
	public void init() throws HibernateException, Exception {
		if (!checaPermissao("gcc__"))
			super.paginaProibida();
	}

	public List<Curso> getCursos() {
		return cursos;
	}

	public List<Curso> getCursosCSV() {
		return cursosCSV;
	}

	@Command
	public void changeEditableStatus(@BindingParam("curso") Curso curso) {
		if (!curso.getEditingStatus()) {
			Curso temp = new Curso();
			temp.copy(curso);
			editTemp.put(curso.getIdCurso(), temp);
			curso.setEditingStatus(true);
		} else {
			curso.copy(editTemp.get(curso.getIdCurso()));
			editTemp.remove(curso.getIdCurso());
			curso.setEditingStatus(false);
		}
		refreshRowTemplate(curso);
	}

	@Command
	public void confirm(@BindingParam("curso") Curso curso) {
		if (cursoBusiness.validate(curso, editTemp.get(curso.getIdCurso())
				.getCodigoCurso())) {
			if (!cursoBusiness.editar(curso))
				Messagebox.show("Não foi possível editar o curso.", "Erro",
						Messagebox.OK, Messagebox.ERROR);
			editTemp.remove(curso.getIdCurso());
			curso.setEditingStatus(false);
			refreshRowTemplate(curso);
		} else {
			String errorMessage = "";
			for (String error : cursoBusiness.getErrors())
				errorMessage += error;
			Messagebox.show(errorMessage, "Dados insuficientes / inválidos",
					Messagebox.OK, Messagebox.ERROR);
			cursoBusiness.clearErrors();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Command
	public void delete(@BindingParam("curso") final Curso curso) {
		Messagebox.show(
				"Você tem certeza que deseja deletar o curso: "
						+ curso.getNomeCurso() + "?", "Confirmação",
				Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION,
				new org.zkoss.zk.ui.event.EventListener() {
					public void onEvent(Event e) {
						if (Messagebox.ON_OK.equals(e.getName())) {

							if (cursoBusiness.exclui(curso)) {
								removeFromList(curso);
								Messagebox.show(
										"O curso foi excluído com sucesso.",
										"Sucesso", Messagebox.OK,
										Messagebox.INFORMATION);
							} else {
								Messagebox
										.show("O curso não foi excluído.",
												"Erro", Messagebox.OK,
												Messagebox.ERROR);
							}

						}
					}
				});
	}

	public void removeFromList(Curso curso) {
		cursos.remove(curso);
		allCursos.remove(curso);
		BindUtils.postNotifyChange(null, null, this, "cursos");
	}

	public void refreshRowTemplate(Curso curso) {
		BindUtils.postNotifyChange(null, null, curso, "editingStatus");
	}

	public String getFilterString() {
		return filterString;
	}

	public void setFilterString(String filterString) {
		this.filterString = filterString;
	}

	@Command
	public void filtra() {
		cursos = new ArrayList<Curso>();
		String filter = filterString.toLowerCase().trim();
		for (Curso c : allCursos) {
			if (c.getNomeCurso().toLowerCase().contains(filter)) {
				cursos.add(c);
			}
		}
		BindUtils.postNotifyChange(null, null, this, "cursos");
	}

	@Command
	public void addCurso(@BindingParam("window") Window window) {
		this.limpa();
		window.doModal();
	}

	public Curso getNovoCurso() {
		return this.novoCurso;
	}

	@Command
	public void submit() {
		if (cursoBusiness.validate(novoCurso, null)) {
			if (cursoBusiness.salvar(novoCurso)) {
				allCursos.add(novoCurso);
				this.filtra();
				BindUtils.postNotifyChange(null, null, this, "cursos");
				Messagebox.show("Curso adicionado com sucesso!", "Sucesso",
						Messagebox.OK, Messagebox.INFORMATION);
				this.limpa();
			} else {
				Messagebox.show("Curso não foi adicionado!", "Erro",
						Messagebox.OK, Messagebox.ERROR);
				cursoBusiness.clearErrors();
			}
		} else {
			String errorMessage = "";
			for (String error : cursoBusiness.getErrors())
				errorMessage += error;
			Messagebox.show(errorMessage, "Dados insuficientes / inválidos",
					Messagebox.OK, Messagebox.ERROR);
			cursoBusiness.clearErrors();
		}
	}

	@Command
	public void importCSV(@BindingParam("evt") UploadEvent evt,
			@BindingParam("window") Window window) {
		Media media = evt.getMedia();
		if (!media.getName().contains(".csv")) {
			Messagebox.show("Apenas CSV são aceitos.", "Arquivo inválido",
					Messagebox.OK, Messagebox.EXCLAMATION);
			return;
		}
		
		String csv = new String(media.getByteData());
		String linhas[] = csv.split("\\r?\\n");

		Curso curso;
		cursosCSV.clear();
		cursosCSV = new ArrayList<Curso>();

		for (String linha : linhas) {
			String campos[] = linha.split(",");
			curso = new Curso(campos[0], campos[1]);
			cursosCSV.add(curso);
		}

		BindUtils.postNotifyChange(null, null, this, "cursosCSV");
		window.doModal();
	}
	
	@NotifyChange("cursosCSV")
	@Command
	public void removeFromCSV(@BindingParam("curso") Curso curso) {
		cursosCSV.remove(curso);
	}

	@NotifyChange("cursos")
	@Command
	public void submitCSV(@BindingParam("window") Window window) {
		CursoDAO cursoDAO = new CursoDAO();
		if (cursosCSV.size() > 0) {
			if (cursoDAO.salvarLista(cursosCSV)) {
				Messagebox.show(cursosCSV.size()
						+ " cursos foram cadastrados com sucesso", "Concluído",
						Messagebox.OK, Messagebox.INFORMATION);				
				allCursos.addAll(cursosCSV);
				cursos = allCursos;
				filtra();
			} else
				Messagebox.show("Os cursos não puderam ser cadastrados", "Erro",
						Messagebox.OK, Messagebox.INFORMATION);
		} else
			Messagebox.show("Nenhum curso foi cadastrado", "Concluído",
					Messagebox.OK, Messagebox.INFORMATION);
		window.onClose();
	}

	public void limpa() {
		cursoBusiness.clearErrors();
		novoCurso = new Curso();
		BindUtils.postNotifyChange(null, null, this, "novoCurso");
	}

}
