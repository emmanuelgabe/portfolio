import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { ProjectListComponent } from './components/project-list/project-list.component';
import { ProjectDetailComponent } from './components/project-detail/project-detail.component';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent
  },
  {
    path: 'about',
    component: HomeComponent // TODO: Remplacer par AboutComponent quand créé (task 1.16)
  },
  {
    path: 'experiences',
    component: HomeComponent // TODO: Remplacer par ExperiencesComponent quand créé (task 7.3)
  },
  {
    path: 'projects',
    component: ProjectListComponent
  },
  {
    path: 'projects/:id',
    component: ProjectDetailComponent
  },
  {
    path: 'contact',
    component: HomeComponent // TODO: Remplacer par ContactComponent quand créé (task 3.5)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
